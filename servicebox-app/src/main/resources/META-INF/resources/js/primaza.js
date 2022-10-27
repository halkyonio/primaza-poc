(() => {
    // EventListener watching submit button event
    // Clean the inner HTML of the <div id="response"> to remove previously displayed messages
    document.addEventListener("submit", () => {
        document.getElementById("response").innerHTML = "";
    });

    // Event watching when HTMX is swapping the HTML content
    // If the div tag includes as class "alert-success",, then the button is disabled
    htmx.on("htmx:afterSwap",function(evt) {
        const msg = evt.detail.elt.outerHTML;
        if (msg.includes("alert-success")) {
            document.getElementById("claim-button").setAttribute("disabled", true);
        };
    });
})();