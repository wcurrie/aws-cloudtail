function getScrollXY() {
    var scrOfX = 0, scrOfY = 0;
    if( typeof( window.pageYOffset ) == 'number' ) {
        //Netscape compliant
        scrOfY = window.pageYOffset;
        scrOfX = window.pageXOffset;
    } else if( document.body && ( document.body.scrollLeft || document.body.scrollTop ) ) {
        //DOM compliant
        scrOfY = document.body.scrollTop;
        scrOfX = document.body.scrollLeft;
    } else if( document.documentElement && ( document.documentElement.scrollLeft || document.documentElement.scrollTop ) ) {
        //IE6 standards compliant mode
        scrOfY = document.documentElement.scrollTop;
        scrOfX = document.documentElement.scrollLeft;
    }
    return [ scrOfX, scrOfY ];
}

// when the mouse moves adjust the y position (vertical offset) of boxes (heads) at the top of each participant timeline
function moveFixed(evt) {
    var scrollpos = getScrollXY();
    var fixed = document.getElementsByClassName("participant-head");
    for (var i = 0; i <fixed.length; i++) {
        var tfm = fixed[i].transform.baseVal.getItem(0);
        tfm.setTranslate(0, scrollpos[1]);
    }
}

window.onscroll = moveFixed;
