(function () {
  "use strict";

  document.querySelectorAll("form[data-guard]").forEach(function (form) {
    let dirty = false;
    form.addEventListener("input", function () {
      dirty = true;
    });
    form.addEventListener("submit", function () {
      dirty = false;
    });
    window.addEventListener("beforeunload", function (event) {
      if (dirty) {
        event.preventDefault();
        event.returnValue = "";
      }
    });
  });

  const sidebar = document.getElementById("sidebar");
  const backdrop = document.getElementById("sidebar-backdrop");
  const toggle = document.getElementById("sidebar-toggle");

  if (!sidebar || !toggle) {
    return;
  }

  const mobileQuery = window.matchMedia("(max-width: 1023px)");

  function applyStoredState() {
    if (mobileQuery.matches) {
      return;
    }
    const collapsed = localStorage.getItem("clinicflow.sidebar") === "collapsed";
    sidebar.classList.toggle("collapsed", collapsed);
    toggle.setAttribute("aria-expanded", String(!collapsed));
  }

  function toggleSidebar() {
    if (mobileQuery.matches) {
      const open = sidebar.classList.toggle("open");
      if (backdrop) {
        backdrop.classList.toggle("show", open);
      }
      toggle.setAttribute("aria-expanded", String(open));
      document.body.classList.toggle("drawer-open", open);
      return;
    }
    const collapsed = sidebar.classList.toggle("collapsed");
    localStorage.setItem("clinicflow.sidebar", collapsed ? "collapsed" : "expanded");
    toggle.setAttribute("aria-expanded", String(!collapsed));
  }

  function closeDrawer() {
    sidebar.classList.remove("open");
    if (backdrop) {
      backdrop.classList.remove("show");
    }
    document.body.classList.remove("drawer-open");
    toggle.setAttribute("aria-expanded", "false");
  }

  toggle.addEventListener("click", toggleSidebar);
  if (backdrop) {
    backdrop.addEventListener("click", closeDrawer);
  }

  document.addEventListener("keydown", function (event) {
    if (event.key === "Escape" && mobileQuery.matches) {
      closeDrawer();
    }
  });

  sidebar.querySelectorAll(".nav-item").forEach(function (link) {
    link.addEventListener("click", function () {
      if (mobileQuery.matches) {
        closeDrawer();
      }
    });
  });

  mobileQuery.addEventListener("change", function () {
    closeDrawer();
    applyStoredState();
  });

  applyStoredState();
})();