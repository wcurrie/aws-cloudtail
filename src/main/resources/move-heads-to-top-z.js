window.addEventListener("load", function() {
  var svg=document.getElementsByTagName("svg");
  var g=svg[0].children[1];
  Array.from(g.getElementsByClassName("participant-head")).forEach(function(head, i) {
    head.parentNode.appendChild(head);
  });
});


