package app.areada.reader.epub

/**
 * Injected into paginated (horizontal swipe) chapters. Lays the chapter out as
 * full-screen CSS columns and moves between them with hardware accelerated
 * translate3d transforms. Communicates with the host through the AreadaPager
 * JavaScript bridge.
 */
internal val EpubPagerScript: String = """
(function () {
  if (window.areadaPagerReady) { return; }
  window.areadaPagerReady = true;

  var MARGIN_X = 22;
  var MARGIN_TOP = 58;
  var MARGIN_BOTTOM = 74;

  var pager = document.createElement('div');
  pager.id = 'areada-pager';
  var body = document.body;
  while (body.firstChild) {
    if (body.firstChild === pager) { break; }
    pager.appendChild(body.firstChild);
  }
  body.appendChild(pager);

  var pageWidth = 1;
  var step = 1;
  var pageCount = 1;
  var index = 0;
  var dragging = false;
  var horizontal = false;
  var moved = false;
  var startX = 0;
  var startY = 0;
  var startTime = 0;
  var dragDx = 0;
  var resizeTimer = null;

  function bridge() {
    return (typeof AreadaPager !== 'undefined') ? AreadaPager : null;
  }

  function fraction() {
    return pageCount > 1 ? index / (pageCount - 1) : 0;
  }

  function apply(animate) {
    pager.style.transition = animate ? 'transform 240ms cubic-bezier(0.22, 0.61, 0.36, 1)' : 'none';
    pager.style.transform = 'translate3d(' + (-index * step) + 'px, 0, 0)';
  }

  function report() {
    var b = bridge();
    if (b && b.progress) {
      try { b.progress(fraction(), pageCount); } catch (e) {}
    }
  }

  function measure(keepFraction) {
    var previous = keepFraction ? fraction() : 0;
    var vw = window.innerWidth || document.documentElement.clientWidth;
    var vh = window.innerHeight || document.documentElement.clientHeight;
    pageWidth = Math.max(80, vw - MARGIN_X * 2);
    step = pageWidth + MARGIN_X * 2;
    pager.style.left = MARGIN_X + 'px';
    pager.style.top = MARGIN_TOP + 'px';
    pager.style.width = pageWidth + 'px';
    pager.style.height = Math.max(80, vh - MARGIN_TOP - MARGIN_BOTTOM) + 'px';
    pager.style.columnWidth = pageWidth + 'px';
    pager.style.webkitColumnWidth = pageWidth + 'px';
    pager.style.columnGap = (MARGIN_X * 2) + 'px';
    pager.style.webkitColumnGap = (MARGIN_X * 2) + 'px';
    pager.style.transition = 'none';
    var total = pager.scrollWidth;
    pageCount = Math.max(1, Math.round((total + MARGIN_X * 2) / step));
    index = Math.max(0, Math.min(pageCount - 1, Math.round(previous * (pageCount - 1))));
    apply(false);
    report();
  }

  function goToIndex(target, animate) {
    var clean = Math.max(0, Math.min(pageCount - 1, target));
    if (clean === index) {
      apply(animate);
      return;
    }
    index = clean;
    apply(animate);
    report();
  }

  function next() {
    if (index < pageCount - 1) {
      goToIndex(index + 1, true);
    } else {
      var b = bridge();
      if (b && b.nextChapter) { try { b.nextChapter(); } catch (e) {} }
    }
  }

  function prev() {
    if (index > 0) {
      goToIndex(index - 1, true);
    } else {
      var b = bridge();
      if (b && b.prevChapter) { try { b.prevChapter(); } catch (e) {} }
    }
  }

  window.areadaNextPage = next;
  window.areadaPrevPage = prev;
  window.areadaGoToFraction = function (value) {
    var clean = Math.max(0, Math.min(1, value));
    goToIndex(Math.round(clean * (pageCount - 1)), false);
  };
  window.areadaRelayout = function () { measure(true); };

  document.addEventListener('touchstart', function (event) {
    if (event.touches.length !== 1) { dragging = false; return; }
    dragging = true;
    horizontal = false;
    moved = false;
    dragDx = 0;
    startX = event.touches[0].clientX;
    startY = event.touches[0].clientY;
    startTime = Date.now();
    pager.style.transition = 'none';
  }, { passive: true });

  document.addEventListener('touchmove', function (event) {
    if (!dragging || event.touches.length !== 1) { return; }
    var dx = event.touches[0].clientX - startX;
    var dy = event.touches[0].clientY - startY;
    if (!horizontal) {
      if (Math.abs(dx) > 8 && Math.abs(dx) > Math.abs(dy)) {
        horizontal = true;
      } else if (Math.abs(dy) > 10) {
        dragging = false;
        return;
      }
    }
    if (!horizontal) { return; }
    if (event.cancelable) { event.preventDefault(); }
    moved = true;
    var offset = dx;
    if ((index === 0 && dx > 0) || (index === pageCount - 1 && dx < 0)) {
      offset = dx * 0.32;
    }
    dragDx = dx;
    pager.style.transform = 'translate3d(' + (-index * step + offset) + 'px, 0, 0)';
  }, { passive: false });

  function finishTouch(event) {
    if (!dragging) { return; }
    dragging = false;
    var elapsed = Math.max(1, Date.now() - startTime);

    if (horizontal && moved) {
      var velocity = dragDx / elapsed;
      if (dragDx < -pageWidth * 0.16 || velocity < -0.45) {
        next();
      } else if (dragDx > pageWidth * 0.16 || velocity > 0.45) {
        prev();
      } else {
        apply(true);
      }
      return;
    }

    var touch = event.changedTouches && event.changedTouches[0];
    if (!touch || elapsed > 420) { return; }
    var target = document.elementFromPoint(touch.clientX, touch.clientY);
    if (target && target.closest && target.closest('a')) { return; }

    var ratio = touch.clientX / (window.innerWidth || 1);
    if (ratio > 0.75) {
      next();
    } else if (ratio < 0.25) {
      prev();
    } else {
      var b = bridge();
      if (b && b.toggleChrome) { try { b.toggleChrome(); } catch (e) {} }
    }
  }

  document.addEventListener('touchend', finishTouch, { passive: true });
  document.addEventListener('touchcancel', function () {
    if (dragging) { dragging = false; apply(true); }
  }, { passive: true });

  document.addEventListener('selectstart', function (event) {
    if (dragging && horizontal) { event.preventDefault(); }
  });

  window.addEventListener('resize', function () {
    if (resizeTimer) { clearTimeout(resizeTimer); }
    resizeTimer = setTimeout(function () { measure(true); }, 120);
  });
  window.addEventListener('orientationchange', function () {
    setTimeout(function () { measure(true); }, 220);
  });

  measure(false);
  if (document.readyState !== 'complete') {
    window.addEventListener('load', function () { measure(true); });
  }
  setTimeout(function () { measure(true); }, 260);
  setTimeout(function () { measure(true); }, 700);
})();
""".trimIndent()
