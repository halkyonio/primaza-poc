(() => {
    // EventListener watching submit button event
    // Clean the inner HTML of the <div id="response"> to remove previously displayed messages
    document.addEventListener("submit", () => {
        var element = document.getElementById("response");
        if (element != null) {
            element.innerHTML = "";
        }
    });

    // Event watching when HTMX is swapping the HTML content
    // If the div tag includes as class "alert-success",, then the button is disabled
    htmx.on("htmx:afterSwap",function(evt) {
        const msg = evt.detail.elt.outerHTML;
        if (msg.includes("alert-success")) {
            var element = document.getElementById("claim-button");
            if (element != null) {
                element.setAttribute("disabled", true);
            }
        };
    });

    // Error handling after validation errors
    htmx.on("htmx:beforeSwap", function(evt) {
        if(evt.detail.xhr.status === 400) {
            // for validation errors, we want to swap the content to the #response element.
            evt.detail.shouldSwap = true;
        }
    });
})();