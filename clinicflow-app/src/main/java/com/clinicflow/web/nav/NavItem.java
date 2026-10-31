package com.clinicflow.web.nav;

public record NavItem(String label, String href, String icon, String matchPrefix) {

    public boolean isActive(String currentPath) {
        if (currentPath == null) {
            return false;
        }
        if (matchPrefix.endsWith("/dashboard")) {
            return currentPath.equals(matchPrefix);
        }
        return currentPath.equals(matchPrefix) || currentPath.startsWith(matchPrefix + "/");
    }
}