# SignalFx client libraries [![Build Status](https://travis-ci.org/signalfx/signalfx-java.svg?branch=master)](https://travis-ci.org/signalfx/signalfx-java)

This repository contains libraries for instrumenting Java applications and
reporting metrics to SignalFx. You will need a SignalFx account and organization
API token to use them. For more information on SignalFx and to create an
account, go to [http://www.signalfx.com](http://www.signalfx.com).

We recommend sending metrics with Java using Codahale Metrics version 3.0+. You
can also use Yammer Metrics 2.0.x (an earlier version of Codahale Metrics). More
information on the Codahale Metrics library can be found on the
[Codahale Metrics website](https://dropwizard.github.io/metrics/).

You can also use the module `signalfx-java` to send metrics directly to SignalFx
using protocol buffers, without using Codahale or Yammer metrics.

## Supported languages

* Java 6+ with `signalfx-metrics`.

## Using this library in your project

If you're using Maven, add the following to your project's `pom.xml` file.

* Codahale 3.0.x

```xml
<dependency>
  <groupId>com.signalfx.public</groupId>
  <artifactId>signalfx-codahale</artifactId>
  <version>0.0.21</version>
</dependency>
```

* Yammer Metrics 2.0.x

```xml
<dependency>
<groupId>com.signalfx.public</groupId>
  <artifactId>signalfx-yammer</artifactId>
  <version>0.0.23</version>
</dependency>
```

If you're using SBT, add the following to your project's `build.sbt` file.

```
libraryDependencies += "com.signalfx.public" % "signalfx-codahale" % "0.0.23"
```

You can also install this library from source by cloning the repo and using
`mvn install` as follows. However, we strongly recommend using the automated
mechanisms described above.

```
$ git clone https://github.com/signalfx/signalfx-java.git
Cloning into 'signalfx-java'...
remote: Counting objects: 930, done.
remote: Compressing objects: 100% (67/67), done.
remote: Total 930 (delta 20), reused 0 (delta 0)
Receiving objects: 100% (930/930), 146.79 KiB | 0 bytes/s, done.
Resolving deltas: 100% (289/289), done.
Checking connectivity... done.
$ cd signalfx-java
$ mvn install
[INFO] Scanning for projects...
...
...
...
[INFO] SignalFx parent .................................. SUCCESS [  2.483 s]
[INFO] SignalFx Protocol Buffer definitions ............. SUCCESS [  5.503 s]
[INFO] SignalFx Protobuf Utilities ...................... SUCCESS [  2.269 s]
[INFO] SignalFx java libraries .......................... SUCCESS [  3.728 s]
[INFO] Codahale to SignalFx ............................. SUCCESS [  2.910 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 17.120 s
[INFO] ------------------------------------------------------------------------
```

## Sending metrics

### Codahale Metrics 3.0.x

#### 1. Set up the Codahale reporter

```java
final MetricRegistry metricRegistery = new MetricRegistry();
final SignalFxReporter signalfxReporter = new SignalFxReporter.Builder(
    metricRegistery,
    "SIGNALFX_AUTH_TOKEN"
).build();
signalfxReporter.start(1, TimeUnit.SECONDS);
final MetricMetadata metricMetadata = signalfxReporter.getMetricMetadata();
```

#### 2. Send a metric

```java
        // This will send the current time in ms to signalfx as a gauge

        metricRegistery.register("gauge", new Gauge<Long>() {
            public Long getValue() {
                return System.currentTimeMillis();
            }
        });
```

#### 3. Add existing dimensions and metadata to metrics

You can add SignalFx specific metadata to Codahale metrics by first gathering
available metadata using `getMetricMetadata()`, then attaching the
MetricMetadata to the metric.

When you use MetricMetadata, call the .register() method you get from the call
forMetric() rather than registering your metric directly with the
metricRegistry.  This will construct a unique Codahale string for your metric.

```java
        // This will send the size of a queue as a gauge, and attach
        // dimension 'queue_name' to the gauge
        final Queue customerQueue = new ArrayBlockingQueue(100);
        metricMetadata.forMetric(new Gauge<Long>() {
            @Override
            public Long getValue() {
                return customerQueue.size();
            }
        }).withDimension("queue_name", "customer_backlog")
                .register(metricRegistery);
```

#### 4. (optional) Add dimensions without knowing if they already exist

We recommend creating your Codahale object as a field of your class, as a
counter or gauge, then using that field to increment values. If you don't want
to maintain this for reasons of code cleanliness, you can create it on the fly
with our builders.

For example, if you wanted a timer that included a dimension indicating which
store it is from, you could use code like this.

```java
        Timer t = metricMetadata.forBuilder(MetricBuilder.TIMERS)
                .withMetricName("request_time")
                .withDimension("storename", "electronics")
                .createOrGet(metricRegistery);

        Timer.Context c = t.time();
        try {
            System.out.println("Doing store things");
        } finally {
            c.close();
        }

        // Java 7 alternative:
//        try (Timer.Context ignored = t.time()) {
//            System.out.println("Doing store things");
//        }

```

#### After setting up Codahale

After setting up a SignalFxReporter, you can use Codahale metrics as you
normally would, reported at the frequency configured by the `SignalFxReporter`.

### Yammer Metrics

You can also use this library with Yammer metrics 2.0.x as shown in the
following examples.

#### 1. Set up Yammer metrics

```java
final MetricRegistry metricRegistery = new MetricRegistry();
final SignalFxReporter signalfxReporter = new SignalFxReporter.Builder(
    metricRegistery,
    "SIGNALFX_AUTH_TOKEN"
).build();
signalfxReporter.start(1, TimeUnit.SECONDS);
final MetricMetadata metricMetadata = signalfxReporter.getMetricMetadata();
```

#### 2. Send a metric with Yammer metrics

```java
        // This will send the current time in ms to signalfx as a gauge

        MetricName gaugeName = new MetricName("group", "type", "gauge");
        Metric gauge = metricRegistery.newGauge(gaugeName, new Gauge<Long>() {
            @Override
            public Long value() {
                return System.currentTimeMillis();
            }
        });
```

#### 3. Add Dimensions and SignalFx metadata to Yammer metrics

Use the MetricMetadata of the reporter as shown.

```java
        final Queue customerQueue = new ArrayBlockingQueue(100);

        MetricName gaugeName = new MetricName("group", "type", "gauge");
        Metric gauge = metricRegistery.newGauge(gaugeName, new Gauge<Integer>()
        {
            @Override
            public Integer value() {
                return customerQueue.size();
            }
        });

        metricMetadata.forMetric(gauge).withDimension("queue_name",
          "customer_backlog");
```

#### 4. Adding Dimensions without knowing if they already exist

This is not supported in Yammer Metrics 2.0.x.

### Changing the default source

The default source name for metrics is discovered by [SourceNameHelper]
(signalfx-java/src/main/java/com/signalfx/metrics/SourceNameHelper.java).
If you want to override the default behavior, you can pass a third parameter to
your Builder and that String is then used as the source.  If you are using AWS,
we provide a helper to extract your AWS instance ID and use that as the source.

For example:

```
final SignalFxReporter signalfxReporter = new SignalFxReporter.Builder(
    metricRegistery,
    "SIGNALFX_AUTH_TOKEN",
    SourceNameHelper.getAwsInstanceId()
).build();
```

## Example Project

You can find a full-stack example project called "signalfx-yammer-example" in
the repo.

Run it as follows:

1. Download the code and create an "auth" file in the "signalfx-yammer-example"
   directory. The auth file should contain the following:

    ```
    auth=<signalfx API Token>
    host=https://ingest.signalfx.com
    ```

2. Run the following commands in your terminal to install and run the example
   project, replacing `path/to/signalfx-yammer-example` with the location of the
   example project code in your environment. You must have Maven installed.

    ```
    cd path/to/signalfx-yammer-example
    mvn install
    mvn exec:java -Dexec.mainClass="com.signalfx.yammer.example.App"
    ```

New metrics from the example project should appear in SignalFx.

## Sending metrics without using Codahale

We recommend sending metrics using Codahale as shown above. You can also
interact with our Java library directly if you do not want to use Codahale. To
do this, you will need to build the metric manually using protocol buffers as
shown in the following example.

```java
        DataPointReceiverEndpoint dataPointEndpoint = new DataPointEndpoint();
        AggregateMetricSender mf =
                new AggregateMetricSender("test.SendMetrics",
                                          new HttpDataPointProtobufReceiverFactory(
                                                  dataPointEndpoint)
                                                  .setVersion(2),
                                          new StaticAuthToken(auth_token),
                                          Collections.<OnSendErrorHandler>singleton(new OnSendErrorHandler() {
                                            @Override
                                            public void handleError(MetricError metricError) {
                                              System.out.println("Unable to POST metrics: " + metricError.getMessage());
                                            }
                                          }));

                                          Collections.<OnSendErrorHandler>emptyList());
      try (AggregateMetricSender.Session i = mf.createSession()) {
          i.setDatapoint(
             SignalFxProtocolBuffers.DataPoint.newBuilder()
               .setMetric("curtime")
               .setValue(
                 SignalFxProtocolBuffers.Datum.newBuilder()
                 .setIntValue(System.currentTimeMillis()))
               .addDimensions(
                 SignalFxProtocolBuffers.Dimension.newBuilder()
                   .setKey("source")
                   .setValue("java"))
               .build());
      }

```
