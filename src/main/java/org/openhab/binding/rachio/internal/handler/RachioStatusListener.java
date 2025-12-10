private final Set<RachioStatusListener> statusListeners = ConcurrentHashMap.newKeySet();

public void registerStatusListener(RachioStatusListener listener) {
    if (listener != null) {
        statusListeners.add(listener);
        logger.debug("Registered status listener: {}", listener.getListenerDescription());
    }
}

public void unregisterStatusListener(RachioStatusListener listener) {
    if (listener != null) {
        statusListeners.remove(listener);
        logger.debug("Unregistered status listener: {}", listener.getListenerDescription());
    }
}

protected void notifyStatusListeners(ThingStatus status, ThingStatusDetail detail, @Nullable String message) {
    for (RachioStatusListener listener : statusListeners) {
        if (listener.isActive()) {
            try {
                listener.onStatusChanged(status, detail, message);
            } catch (Exception e) {
                logger.warn("Error notifying status listener {}", listener.getListenerDescription(), e);
            }
        }
    }
}

protected void notifyDeviceUpdated(String deviceId) {
    for (RachioStatusListener listener : statusListeners) {
        if (listener.isActive() && (listener.isForDevice(deviceId) || listener.isForThing(deviceId))) {
            try {
                listener.onDeviceUpdated(deviceId);
            } catch (Exception e) {
                logger.warn("Error notifying device update to listener {}", listener.getListenerDescription(), e);
            }
        }
    }
}

protected void notifyZoneUpdated(String zoneId) {
    for (RachioStatusListener listener : statusListeners) {
        if (listener.isActive() && listener.isForZone(zoneId)) {
            try {
                listener.onZoneUpdated(zoneId);
            } catch (Exception e) {
                logger.warn("Error notifying zone update to listener {}", listener.getListenerDescription(), e);
            }
        }
    }
}
