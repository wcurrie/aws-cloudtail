Array.from(document.getElementsByClassName("participant-head")).forEach((head) => {
  head.addEventListener("click", () => {
    let actor = head.getElementsByTagName("text")[0].textContent;
    let offsets = msgYOffsets[actor];
    let nextMsg = offsets.find((e) => e.position > window.scrollY);
    if (nextMsg) {
      window.scroll(window.scrollX, nextMsg.position - 50);
    }
  })
});