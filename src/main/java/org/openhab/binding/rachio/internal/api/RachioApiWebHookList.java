package org.openhab.binding.rachio.internal.api;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link RachioApiWebHookList} class defines the webhook list response from the Rachio API
 *
 * @author Damien Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioApiWebHookList {

    @SerializedName("data")
    private @Nullable List<RachioApiWebHookEntry> data;

    @SerializedName("total")
    private int total = 0;

    @SerializedName("page")
    private int page = 1;

    @SerializedName("pageSize")
    private int pageSize = 50;

    @SerializedName("hasMore")
    private boolean hasMore = false;

    /**
     * Default constructor
     */
    public RachioApiWebHookList() {
        this.data = new ArrayList<>();
    }

    /**
     * Constructor with data
     *
     * @param data webhook entries
     */
    public RachioApiWebHookList(List<RachioApiWebHookEntry> data) {
        this.data = data != null ? new ArrayList<>(data) : new ArrayList<>();
        this.total = this.data.size();
    }

    /**
     * Get the list of webhook entries
     *
     * @return list of webhook entries
     */
    public List<RachioApiWebHookEntry> getData() {
        List<RachioApiWebHookEntry> localData = data;
        return localData != null ? localData : new ArrayList<>();
    }

    /**
     * Set the list of webhook entries
     *
     * @param data list of webhook entries
     */
    public void setData(@Nullable List<RachioApiWebHookEntry> data) {
        this.data = data != null ? new ArrayList<>(data) : new ArrayList<>();
        this.total = this.data.size();
    }

    /**
     * Get total number of webhooks
     *
     * @return total count
     */
    public int getTotal() {
        return total;
    }

    /**
     * Set total number of webhooks
     *
     * @param total total count
     */
    public void setTotal(int total) {
        this.total = total;
    }

    /**
     * Get current page number
     *
     * @return page number
     */
    public int getPage() {
        return page;
    }

    /**
     * Set current page number
     *
     * @param page page number
     */
    public void setPage(int page) {
        this.page = page;
    }

    /**
     * Get page size
     *
     * @return page size
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Set page size
     *
     * @param pageSize page size
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Check if there are more pages
     *
     * @return true if more pages available
     */
    public boolean hasMore() {
        return hasMore;
    }

    /**
     * Set if there are more pages
     *
     * @param hasMore true if more pages available
     */
    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    /**
     * Get webhook entry by index
     *
     * @param index index
     * @return webhook entry or null
     */
    public @Nullable RachioApiWebHookEntry get(int index) {
        List<RachioApiWebHookEntry> localData = data;
        if (localData != null && index >= 0 && index < localData.size()) {
            return localData.get(index);
        }
        return null;
    }

    /**
     * Get webhook entry by ID
     *
     * @param id webhook ID
     * @return webhook entry or null
     */
    public @Nullable RachioApiWebHookEntry getById(String id) {
        List<RachioApiWebHookEntry> localData = data;
        if (localData != null && id != null) {
            for (RachioApiWebHookEntry entry : localData) {
                if (id.equals(entry.getId())) {
                    return entry;
                }
            }
        }
        return null;
    }

    /**
     * Check if list contains a webhook with given ID
     *
     * @param id webhook ID
     * @return true if found
     */
    public boolean containsId(String id) {
        return getById(id) != null;
    }

    /**
     * Get webhook entry by URL
     *
     * @param url webhook URL
     * @return webhook entry or null
     */
    public @Nullable RachioApiWebHookEntry getByUrl(String url) {
        List<RachioApiWebHookEntry> localData = data;
        if (localData != null && url != null) {
            for (RachioApiWebHookEntry entry : localData) {
                if (url.equals(entry.getUrl())) {
                    return entry;
                }
            }
        }
        return null;
    }

    /**
     * Check if list contains a webhook with given URL
     *
     * @param url webhook URL
     * @return true if found
     */
    public boolean containsUrl(String url) {
        return getByUrl(url) != null;
    }

    /**
     * Add a webhook entry
     *
     * @param entry webhook entry
     */
    public void add(RachioApiWebHookEntry entry) {
        List<RachioApiWebHookEntry> localData = data;
        if (localData == null) {
            localData = new ArrayList<>();
            data = localData;
        }
        localData.add(entry);
        total = localData.size();
    }

    /**
     * Remove a webhook entry by index
     *
     * @param index index
     * @return removed entry or null
     */
    public @Nullable RachioApiWebHookEntry remove(int index) {
        List<RachioApiWebHookEntry> localData = data;
        if (localData != null && index >= 0 && index < localData.size()) {
            RachioApiWebHookEntry removed = localData.remove(index);
            total = localData.size();
            return removed;
        }
        return null;
    }

    /**
     * Remove a webhook entry by ID
     *
     * @param id webhook ID
     * @return true if removed
     */
    public boolean removeById(String id) {
        List<RachioApiWebHookEntry> localData = data;
        if (localData != null && id != null) {
            for (int i = 0; i < localData.size(); i++) {
                RachioApiWebHookEntry entry = localData.get(i);
                if (id.equals(entry.getId())) {
                    localData.remove(i);
                    total = localData.size();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Clear all webhook entries
     */
    public void clear() {
        List<RachioApiWebHookEntry> localData = data;
        if (localData != null) {
            localData.clear();
            total = 0;
        }
    }

    /**
     * Get number of webhook entries
     *
     * @return size of list
     */
    public int size() {
        List<RachioApiWebHookEntry> localData = data;
        return localData != null ? localData.size() : 0;
    }

    /**
     * Check if list is empty
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        List<RachioApiWebHookEntry> localData = data;
        return localData == null || localData.isEmpty();
    }

    /**
     * Get string representation
     *
     * @return string representation
     */
    @Override
    public String toString() {
        List<RachioApiWebHookEntry> localData = data;
        return "RachioApiWebHookList{" +
                "data=" + (localData != null ? localData.size() : 0) + " entries" +
                ", total=" + total +
                ", page=" + page +
                ", pageSize=" + pageSize +
                ", hasMore=" + hasMore +
                '}';
    }

    /**
     * Get detailed string representation
     *
     * @return detailed string
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RachioApiWebHookList [");
        sb.append("total=").append(total);
        sb.append(", page=").append(page);
        sb.append("/").append(getTotalPages());
        sb.append(", pageSize=").append(pageSize);
        sb.append(", hasMore=").append(hasMore);
        sb.append("]\n");
        
        List<RachioApiWebHookEntry> localData = data;
        if (localData != null && !localData.isEmpty()) {
            for (int i = 0; i < localData.size(); i++) {
                RachioApiWebHookEntry entry = localData.get(i);
                sb.append("  [").append(i).append("] ").append(entry.toString()).append("\n");
            }
        } else {
            sb.append("  No webhooks\n");
        }
        
        return sb.toString();
    }

    /**
     * Calculate total pages
     *
     * @return total pages
     */
    public int getTotalPages() {
        if (total == 0 || pageSize == 0) {
            return 0;
        }
        return (int) Math.ceil((double) total / pageSize);
    }
}
