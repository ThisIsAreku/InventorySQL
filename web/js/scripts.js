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
		current_userid = $(this).data('userid');
		current_username = $(this).data('username');
		$('#user-summary h1').text(current_username);
		$('#user-summary p').text("Loading data for " + current_username + "...");
	});
});