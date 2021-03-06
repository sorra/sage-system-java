'use strict';

$(tag_input_init)

function getTagChain(id, $parent) {
	$.get('/tag/card/'+id, {})
	.done(function(resp){
		createTagChain(resp).appendTo($parent);
	})
	.fail(function(resp){
		console.log(resp);
	});
}

function createTagChain(tagCard) {
	var $tch = $('<div class="tag-chain"></div>').css({position: 'relative'});
	for (var i = tagCard.chainUp.length-1, inc = 0; i >= 0; i--, inc++) {
		var item = tagCard.chainUp[i];

		var $tag = $('<a></a>').addClass('tag btn').addClass('btn-info').appendTo($tch);
		$tag.text(item.name).attr('href', '/public/'+item.id);
		$tag.css({display:	'block',
				  width:	'58px',
				  height:	'23px',
				  padding:	'0',
				  margin:	'0'});
		var pleft = inc*(60+50);
		$tag.css({position:	'absolute',
				  left: pleft+'px',
				  top: '0px'});
		if (i == 0) {
			$tag.removeClass('btn-info').addClass('btn-success');
		}
		
		if (i > 0) {
      var pxleft = inc*(60+50) + 60;
			var $line = $('<div></div>').addClass('line').appendTo($tch);
			$line.css({width:	'50px',
					   height:	'5px',
					   background:	'#CCCCCC'})
			     .css({position: 'absolute',
					   left: pxleft+'px',
					   top: '10px'});
		}
	};
	return $tch;
}

function createTagLabel(tagLabel) {
	var $tl = $('<a class="tag-label" title=""></a>');
	$tl.text(tagLabel.name)
	   .attr('tag-id', tagLabel.id)
	   .attr('href', '/public/'+tagLabel.id)
	   .click(function(event) {
	     event.preventDefault()
	     window.open('/public/'+tagLabel.id)
	   });
	if (tagLabel.chainStr) {
		$tl.attr('title', tagLabel.chainStr);
	}
	return $tl;
}

function tag_tree(tagTree, params) {
  var $tree = $('<div class="tag-tree">')
	tag_node($tree, tagTree, -1, params)
	return $tree
}

function tag_node($tree, tag, depth, params) {
	if (depth >= 0) {
		var $tag = $('<a class="tag-label btn btn-default">').appendTo($tree)
			.attr({title: tag.chainStr, 'tag-id': tag.id, 'href': '/public/'+tag.id})
			.text(tag.name)
			.css('margin-left', (30*depth) + 'px')
		if (depth <= 0) {
			$tag.css('margin-top', '10px')
		}
		if (params && params.asTagInput) {
			$tag.removeClass('tag-label').addClass('tag-sel').removeAttr('href')
		}
		$('<br>').appendTo($tree)
	}

	for (var i in tag.children) {
		tag_node($tree, tag.children[i], depth+1, params)
	}
}

function hideTagTreeInput($tagPlus){
	$tagPlus.removeData('tree-on')
	$tagPlus.data('bs.popover').tip().find('.tag-sel').removeClass('btn-success')
	$tagPlus.popover('hide')
}

function tag_input_init() {
	$(document).delegate('.tag-sel', 'click', function(){
	  $(this).toggleClass('btn-success')
	})

	$('.tag-plus').popover({
		html: true,
		placement: 'bottom',
		trigger: 'manual',
		content: $('<div>')
	}).each(function(){
		var $po = $(this).data('bs.popover').tip()
		$po.find('.tag-tree').remove()
		tag_tree(window.tagTree, {asTagInput: true}).appendTo($po)
	})

	$(document).delegate('.tag-plus','click', function(){
	  if (!window.tagTree) {
	    $.get('/tag/tree').done(function(resp){
	      window.tagTree = resp
	    }).fail(function(err){
	      console.error("/tag/tree fails: " + err)
	    })
	  }
    var $this = $(this)
    if ($this.data('tree-on') == true) {
			hideTagTreeInput($this)
    } else {
      $this.data('tree-on', true)
      $this.popover('show')
    }
	})

	$(document).delegate('.tag-clear', "click", function(){
	  $(this).parents('.tag-input').find('.tag-sel').removeClass('btn-success')
	})
}