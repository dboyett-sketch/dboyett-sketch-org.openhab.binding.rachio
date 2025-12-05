public class RachioBridgeHandler extends BaseBridgeHandler {
    private final List<RachioStatusListener> listeners = new CopyOnWriteArrayList<>();
    
    public void registerListener(RachioStatusListener listener) {
        // Sort by priority (highest first)
        listeners.add(listener);
        listeners.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
    }
    
    public void notifyZoneStatusChanged(RachioZone zone) {
        for (RachioStatusListener listener : listeners) {
            if (listener.isInterestedInZone(zone.id)) {
                listener.zoneStatusChanged(zone);
            }
        }
    }
    
    // Similar methods for other event types
}
