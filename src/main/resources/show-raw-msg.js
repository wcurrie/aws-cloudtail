
function showRawMsg(index) {
  var msg = rawMsgs[index];
  document.getElementById("popupcontent").textContent = JSON.stringify(msg, null, "  ");
  document.getElementById("popup").style.display = 'block';
  if (hljs) {
    hljs.highlightBlock(document.getElementById("popupcontent"))
  }
}

document.getElementById("popupclose").onclick = function() {
  document.getElementById("popup").style.display = 'none';
};

window.addEventListener("keydown", function (event) {
  if (event.defaultPrevented) {
    return;
  }

  if (event.key === "Escape" && document.getElementById("popup").style.display === 'block') {
    document.getElementById("popup").style.display = 'none';
    event.preventDefault();
  }
}, true);