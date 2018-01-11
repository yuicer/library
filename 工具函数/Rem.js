(function (doc, win) {
  var user_webset_font = getComputedStyle(doc.documentElement, false)['fontSize']
  var rate = parseFloat(user_webset_font) / 16
  var recalc = function () {
    var docEl = doc.documentElement
    var clientWidth = docEl.clientWidth
    if (!clientWidth) return
    docEl.style.fontSize = (100 * (clientWidth / 375)) / rate + 'px'
  }
  if (!doc.addEventListener) return
  var resizeEvt = 'orientationchange' in window ? 'orientationchange' : 'resize'
  win.addEventListener(resizeEvt, recalc, false)
  doc.addEventListener('DOMContentLoaded', recalc, false)
})(document, window)