/*
  Purpose: allow navigation to nearest offscreen message for each participant in the sequence diagram.
  Show up/down arrow icons near each participant line on mouseover. Only show an arrow if there is a message in the
  relevant (up or down) direction.
 */

var upArrow = Snap().path("M434.252,208.708L248.387,22.843c-7.042-7.043-15.693-10.564-25.977-10.564c-10.467,0-19.036,3.521-25.697,10.564 L10.848,208.708C3.615,215.94,0,224.604,0,234.692c0,9.897,3.619,18.459,10.848,25.693l21.411,21.409 c6.854,7.231,15.42,10.855,25.697,10.855c10.278,0,18.842-3.624,25.697-10.855l83.939-83.651v200.998 c0,9.89,3.567,17.936,10.706,24.126c7.139,6.184,15.752,9.273,25.837,9.273h36.545c10.089,0,18.698-3.09,25.837-9.273 c7.139-6.188,10.712-14.236,10.712-24.126V198.144l83.938,83.651c6.848,7.231,15.413,10.855,25.7,10.855 c10.082,0,18.747-3.624,25.975-10.855l21.409-21.409c7.043-7.426,10.567-15.988,10.567-25.693 C444.819,224.795,441.295,216.134,434.252,208.708z");
var downArrow = Snap().path("M434.252,185.721l-21.409-21.413c-7.419-7.042-16.084-10.564-25.975-10.564c-10.095,0-18.657,3.521-25.7,10.564 l-83.938,83.939V47.255c0-9.9-3.621-18.464-10.855-25.697c-7.234-7.232-15.797-10.85-25.693-10.85h-36.545 c-9.897,0-18.464,3.621-25.693,10.85c-7.236,7.233-10.85,15.797-10.85,25.697v200.992l-83.939-83.939 c-7.042-7.042-15.606-10.564-25.697-10.564c-9.896,0-18.559,3.521-25.979,10.564l-21.128,21.413C3.615,192.948,0,201.615,0,211.7 c0,10.282,3.619,18.848,10.848,25.698l185.864,186.146c7.045,7.046,15.609,10.567,25.697,10.567 c9.897,0,18.558-3.521,25.977-10.567l185.865-186.146c7.043-7.043,10.567-15.608,10.567-25.698 C444.819,201.805,441.295,193.145,434.252,185.721z");

function msgsCloseTo(actor, y) {
  let offsets = msgYOffsets[actor];
  let nextIndexDown = offsets.findIndex((e) => e.position > y);
  if (nextIndexDown !== -1) {
    return {
      down: offsets[nextIndexDown].position,
      up: (nextIndexDown - 1) < 0 ? -1 : offsets[nextIndexDown - 1].position
    };
  } else {
    return {
      down: -1,
      up: offsets[offsets.length - 1].position
    }
  }
}

var removeArrowsTimeout;
function scheduleRemoveArrows() {
  removeArrowsTimeout = setTimeout(() => {
    upArrow.remove();
    downArrow.remove();
  }, 500);
}
function clearRemoveArrowsTimeout() {
  if (removeArrowsTimeout) {
    clearTimeout(removeArrowsTimeout);
  }
}
upArrow.mouseover(clearRemoveArrowsTimeout);
downArrow.mouseover(clearRemoveArrowsTimeout);
upArrow.mouseout(scheduleRemoveArrows);
downArrow.mouseout(scheduleRemoveArrows);

var currentActor;
function scrollYOnClick(arrow, direction) {
  arrow.click((e) => {
    if (currentActor) {
      var closeMsgs = msgsCloseTo(currentActor, e.pageY);
      var relevantY = closeMsgs[direction];
      if (relevantY !== -1) {
        window.scroll(window.scrollX, relevantY - 50);
      }
    }
  });
}

scrollYOnClick(upArrow, "up");
scrollYOnClick(downArrow, "down");

var svg = Snap(document.getElementsByTagName("svg")[0]);
var lines = svg.selectAll(".participant-line");
lines.forEach((g) => {
  var line = g.children()[0];
  var actor = g.attr("id").replace("line-for-", "");
  var x = +line.attr("x1");
  var mouseBox = Snap().rect(x - 5, line.attr("y1"), 10, line.attr("y2") - line.attr("y1"));
  mouseBox.attr("opacity", "0");
  mouseBox.mouseover((e) => {
    clearRemoveArrowsTimeout();
    currentActor = actor;
    var closeMsgs = msgsCloseTo(currentActor, e.pageY);
    var y = e.pageY - 10;
    if (closeMsgs.up !== -1 && closeMsgs.up < window.scrollY) {
      upArrow.attr("transform", "translate(" + (x + 4) + "," + y + ") scale(0.05, 0.05)");
      svg.append(upArrow);
    } else {
      upArrow.remove();
    }
    if (closeMsgs.down !== -1 && closeMsgs.down > (window.scrollY + window.innerHeight)) {
      downArrow.attr("transform", "translate(" + (x - 25) + "," + y + ") scale(0.05, 0.05)");
      svg.append(downArrow);
    } else {
      downArrow.remove();
    }
  });
  mouseBox.mouseout(scheduleRemoveArrows);
  mouseBox.insertAfter(line);
});