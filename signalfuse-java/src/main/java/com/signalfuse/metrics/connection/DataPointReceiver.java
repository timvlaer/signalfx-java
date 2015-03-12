package com.signalfx.metrics.connection;

import java.util.List;
import java.util.Map;

import com.signalfx.metrics.SignalfuseMetricsException;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

public interface DataPointReceiver {
    void addDataPoints(String auth, List<SignalFxProtocolBuffers.DataPoint> dataPoints)
            throws SignalfuseMetricsException;

    void backfillDataPoints(String auth, String source, String metric,
                            List<SignalFxProtocolBuffers.Datum> datumPoints)
            throws SignalfuseMetricsException;

    Map<String, Boolean> registerMetrics(String auth, Map<String, SignalFxProtocolBuffers.MetricType> metricTypes)
            throws SignalfuseMetricsException;
}
