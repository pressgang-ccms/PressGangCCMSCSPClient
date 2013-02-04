package org.jboss.pressgang.ccms.contentspec.client.entities;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Spec {
    private Integer id = 0;
    private String title = null;
    private String product = null;
    private String version = null;
    private String creator = null;
    private Date lastModified = null;

    public Spec(final Integer id, final String title, final String product, final String version, final String creator,
            final Date lastModified) {
        this.id = id;
        this.title = title;
        this.product = product;
        this.version = version;
        this.creator = creator;
        this.lastModified = lastModified;
    }

    public Spec() {

    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(final String product) {
        this.product = product;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(final Date lastModified) {
        this.lastModified = lastModified;
    }

    public String toString() {
        final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMM yyyy");
        return String.format("ID: %s, Title: %s, Product: %s, Version: %s, Created By: %s, Last Modified: %s", Integer.toString(id), title,
                product, version, creator, dateFormatter.format(lastModified));
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(final String creator) {
        this.creator = creator;
    }
}