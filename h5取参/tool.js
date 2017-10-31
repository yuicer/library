function GetQueryString(name) {
	var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
	var r = window.location.search.substr(1).match(reg);
	if (r != null) return unescape(r[2]);
	return null;
}

function GetCookie() {
	for (var a = document.cookie.split(';'), i = 0; i < a.length; i++) {
		if (a[i].match(/cookie/i)) {
			var cookie = a[i].split('=')[1];
			return cookie;
		}
	}
}

function GoScroll(rate) {
	var speed = document.body.clientHeight / rate,
		interval = setInterval(() => {
			var old = document.body.scrollTop;
			document.body.scrollTop += speed;
			if (document.body.scrollTop <= old)
				clearInterval(interval);
		}, 16)
},
