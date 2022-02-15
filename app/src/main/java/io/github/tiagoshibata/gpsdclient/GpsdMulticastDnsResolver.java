package io.github.tiagoshibata.gpsdclient;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

public class GpsdMulticastDnsResolver implements NsdManager.DiscoveryListener {
    private static final String TAG = "GpsdMulticastDnsReslvr";

    private NsdManager nsdManager;
    private ResolveListener resolveListener;
    private String serviceType;
    private String serviceName;
    private String resolvedAddress;
    private LoggingCallback loggingCallback;


    public GpsdMulticastDnsResolver(NsdManager nsdManager, String serviceType, String serviceName) {
        this.nsdManager = nsdManager;
        this.resolveListener = new ResolveListener(this);
        this.serviceType = serviceType;
        if(serviceName.endsWith(".local")) {
            serviceName =  serviceName.replaceAll("\\.local$", "");
        }
        this.serviceName = serviceName;
        this.resolvedAddress = null;
    }


    public String resolve() {
        nsdManager.discoverServices(
                serviceType, NsdManager.PROTOCOL_DNS_SD, this
        );

        int pollingAttempts = 0;
        while(resolvedAddress == null && pollingAttempts < 30) {
            pollingAttempts++;
            try {
                Thread.sleep(300);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }

        return resolvedAddress;
    }

    public void onStartDiscoveryFailed(String s, int i) {}
    public void onStopDiscoveryFailed(String s, int i) {}
    public void onDiscoveryStarted(String s) {}
    public void onDiscoveryStopped(String s) {}
    public void onServiceFound(NsdServiceInfo info) {

        log("onServiceFound: " + info.getServiceType() + ", " + info.getServiceName());
        nsdManager.resolveService(info, resolveListener);
    }
    public void onServiceLost(NsdServiceInfo info) {}

    private void setResolvedAddress(String host) {
        resolvedAddress = host;
    }

    private class ResolveListener implements NsdManager.ResolveListener {

        public ResolveListener(GpsdMulticastDnsResolver parent) {
            this.parent = parent;
        }

        private GpsdMulticastDnsResolver parent;

        @Override
        public void onResolveFailed(NsdServiceInfo info, int i) {
            parent.log("onResolveFailed: " + info.getServiceType() + ", " + info.getServiceName());
        }

        @Override
        public void onServiceResolved(NsdServiceInfo info) {
            String host = "null";
            if(info.getHost() != null) {
                host = info.getHost().toString();
                if(host.startsWith("/")) {
                    host = host.replaceAll("^/", "");
                }
            }

            parent.log("onServiceResolved: " + info.getServiceType() + ", " + info.getServiceName() + ", " + host);

            if(info.getServiceName().equals(parent.serviceName) && info.getHost() != null) {
                parent.setResolvedAddress(host);
            }
        }
    }


    public void setLoggingCallback(LoggingCallback loggingCallback) {
        this.loggingCallback = loggingCallback;
    }

    private void log(String message) {
        Log.i(TAG, message);
        if (loggingCallback != null)
            loggingCallback.log(message);
    }

}
