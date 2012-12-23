var current_userid;
var current_username;
$(function(){
	$.getJSON('./api.php',{'p': 'versions'}, function(data){
		$('#versions-display .plugin span').text(data.plugin);
		$('#versions-display .web span').text(data.web);
	});
	$.getJSON('./api.php',{'p': 'users'}, function(data){
		$.each(data.users, function(index, value) {
			$('#users-sidelist').append('<li class="user-selector"><a data-userid="'+index+'" data-username="'+value+'" href="#/u/'+index+'-'+value+'">'+value+'</a></li>');
		});
		$('#users-sidelist .first-header').text('Users');
	});
	$('#users-sidelist').on("click", "li.user-selector a", function(event){
		$('#users-sidelist li.user-selector.active').removeClass('active');
		$(this).parent().addClass('active');
		current_userid = $(this).data('userid');
		current_username = $(this).data('username');
		$('#user-summary h1').text(current_username);
		$('#user-summary p').text("Loading data for " + current_username + "...");
		load_inv();
		load_pend();
		$('#user-data').addClass('in');
	});
	$('#inventory-table').on("click", "td.clickable", function(event){
		$('#slot-detail').parent().addClass('in');
		$('#slot-detail .name').text($(this).data('name'));
		$('#slot-detail .id').text($(this).data('id'));
		$('#slot-detail .data').text($(this).data('data'));
		$('#slot-detail .damage').text($(this).data('damage'));
		$('#slot-detail .count').text($(this).data('count'));
	});
	$('#pendings-table').on("click", "a.details", function(event){
		event.preventDefault();
	});
	$('#reload-inventory').click(function(event){
		event.preventDefault();
		$(this).children('i').addClass('ani rotate');
		setTimeout(function(){
			$('#reload-inventory i').removeClass('ani rotate');
		}, 2000);
		load_inv();
	});
	$('#reload-pendings').click(function(event){
		event.preventDefault();
		$(this).children('i').addClass('ani rotate');
		setTimeout(function(){
			$('#reload-pendings i').removeClass('ani rotate');
		}, 2000);
		load_pend();
	});
});
function load_inv()
{
	$('#inventory-table td').removeClass('clickable').text('');
	$.getJSON('./api.php',{'p': 'inv','u': current_userid,'w': 'world'}, function(data){
		$.each(data.inv, function(index, value) {
			$el = $('#inventory-table .slot-'+value.slot);
			$el.addClass('clickable').text(value.item);
			$el.data('slotid', value.slot); //trick to not have to type every data attribute in html..
			$el.data('name', value.item_name);
			$el.data('data', value.data);
			$el.data('damage', value.damage);
			$el.data('count', value.count);
			$el.data('id', value.item);
			
		});
	});
}
function load_pend()
{
	$('#pendings-table tbody').empty();
	$.getJSON('./api.php',{'p': 'pendings','u': current_userid,'w': 'world'}, function(data){
		$.each(data.pendings, function(index, value) {
			$el = $('#pendings-table tbody').append('<tr><td>'+value.item_name+'</td><td>'+value.item+'</td><td>'+value.data+'</td><td>'+value.damage+'</td><td>'+value.count+'</td><td><a href="#" class="details">details</a></td></tr>');
			$el.data('name', value.item_name);
			$el.data('data', value.data);
			$el.data('damage', value.damage);
			$el.data('count', value.count);
			$el.data('id', value.item);
			
		});
	});
}