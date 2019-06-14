package com.good.automated.test.screenFinder.checker;

import com.good.automated.test.screenFinder.handlers.ViewHandler;

/**
 * A listener for view appearance events.
 */
public class ViewListener implements IViewListener {

    private Object lock;
    private volatile boolean found = false;

    private ViewHandler handler;

    public ViewListener(ViewHandler handler) {
        this.handler = handler;
    }

    @Override
    public void onViewFound() {
        if (lock != null) {
            found = true;
            lock.notify();
        }
    }

    /**
     * Checks if a specified UI resource if shown.
     *
     * @param packageName   package name of tha app resource owner
     * @param resourceId    id of resource to check for presence
     * @param delay         timeout to wait until treat check as failed
     * @return              true - if specified resource was found within timeframe
     *                      false - otherwise
     */
    public boolean isScreenShown(String packageName, String resourceId, long delay) {

        if (handler == null)
            return false;

        ViewChecker viewChecker = new ViewChecker(packageName, resourceId, this);
        lock = new Object();
        handler.setChecker(viewChecker);
        try {
            lock.wait(delay);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } finally {
            lock = null;
            handler.setChecker(null);
        }

        return found;
    }

}
