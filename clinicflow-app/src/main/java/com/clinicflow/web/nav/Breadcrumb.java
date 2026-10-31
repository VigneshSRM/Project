package com.clinicflow.web.nav;

public record Breadcrumb(String label, String href) {

    public boolean isCurrent() {
        return href == null;
    }
}