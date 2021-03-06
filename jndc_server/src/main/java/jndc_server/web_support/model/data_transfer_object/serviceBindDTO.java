package jndc_server.web_support.model.data_transfer_object;

public class serviceBindDTO {
    private String serverPortId;

    private String serviceId;

    private String enableDateRange;

    @Override
    public String toString() {
        return "serviceBindDTO{" +
                "serverPortId='" + serverPortId + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", enableDateRange='" + enableDateRange + '\'' +
                '}';
    }

    public String getEnableDateRange() {
        return enableDateRange;
    }

    public void setEnableDateRange(String enableDateRange) {
        this.enableDateRange = enableDateRange;
    }

    public String getServerPortId() {
        return serverPortId;
    }

    public void setServerPortId(String serverPortId) {
        this.serverPortId = serverPortId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }
}
