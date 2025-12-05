public class MyHandler extends RachioHandler implements RachioStatusListener {
    
    @Override
    public void initialize() {
        initializeCommon(); // Call base initialization
        registerWithBridge(); // Register as listener
        startPolling(); // Start polling
        // Subclass-specific initialization
    }
    
    @Override
    protected void poll() {
        // Implementation of polling logic
    }
    
    @Override
    public void refresh() {
        // Implementation of refresh logic
    }
    
    @Override
    protected @Nullable String getDeviceId() {
        // Return device ID for this handler
    }
    
    @Override
    public void dispose() {
        unregisterFromBridge(); // Unregister from bridge
        super.dispose(); // Call base disposal
    }
}
