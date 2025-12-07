package org.openhab.binding.rachio.internal.api;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO representing a list of webhooks from the Rachio API
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioApiWebHookList {
    
    // CRITICAL: This field was missing and causing compilation errors
    @SerializedName("data")
    private List<RachioApiWebHookEntry> data = List.of();
    
    @SerializedName("status")
    private String status = "";
    
    @SerializedName("message")
    private String message = "";
    
    @SerializedName("code")
    private Integer code = 0;
    
    @SerializedName("total")
    private Integer total = 0;
    
    @SerializedName("page")
    private Integer page = 1;
    
    @SerializedName("pageSize")
    private Integer pageSize = 50;
    
    @SerializedName("hasMore")
    private Boolean hasMore = false;
    
    // Getters and setters
    public List<RachioApiWebHookEntry> getData() {
        return data;
    }
    
    public void setData(List<RachioApiWebHookEntry> data) {
        this.data = data;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Integer getCode() {
        return code != null ? code : 0;
    }
    
    public void setCode(Integer code) {
        this.code = code;
    }
    
    public Integer getTotal() {
        return total != null ? total : 0;
    }
    
    public void setTotal(Integer total) {
        this.total = total;
    }
    
    public Integer getPage() {
        return page != null ? page : 1;
    }
    
    public void setPage(Integer page) {
        this.page = page;
    }
    
    public Integer getPageSize() {
        return pageSize != null ? pageSize : 50;
    }
    
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
    
    public Boolean getHasMore() {
        return hasMore != null ? hasMore : false;
    }
    
    public void setHasMore(Boolean hasMore) {
        this.hasMore = hasMore;
    }
    
    // Helper methods
    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status) || 
               "ok".equalsIgnoreCase(status) || 
               (code != null && code >= 200 && code < 300);
    }
    
    public boolean isEmpty() {
        return data == null || data.isEmpty();
    }
    
    public int size() {
        return data != null ? data.size() : 0;
    }
    
    @Nullable
    public RachioApiWebHookEntry getFirst() {
        return !isEmpty() ? data.get(0) : null;
    }
    
    @Nullable
    public RachioApiWebHookEntry findById(String id) {
        if (data == null || id == null) {
            return null;
        }
        for (RachioApiWebHookEntry entry : data) {
            if (id.equals(entry.getId())) {
                return entry;
            }
        }
        return null;
    }
    
    @Nullable
    public RachioApiWebHookEntry findByExternalId(String externalId) {
        if (data == null || externalId == null) {
            return null;
        }
        for (RachioApiWebHookEntry entry : data) {
            if (externalId.equals(entry.getExternalId())) {
                return entry;
            }
        }
        return null;
    }
    
    @Nullable
    public RachioApiWebHookEntry findByUrl(String url) {
        if (data == null || url == null) {
            return null;
        }
        for (RachioApiWebHookEntry entry : data) {
            if (url.equals(entry.getUrl())) {
                return entry;
            }
        }
        return null;
    }
    
    public boolean containsId(String id) {
        return findById(id) != null;
    }
    
    public boolean containsExternalId(String externalId) {
        return findByExternalId(externalId) != null;
    }
    
    public boolean containsUrl(String url) {
        return findByUrl(url) != null;
    }
    
    @Override
    public String toString() {
        return String.format("RachioApiWebHookList[status=%s, code=%d, total=%d, items=%d]", 
            status, code, total, size());
    }
}
