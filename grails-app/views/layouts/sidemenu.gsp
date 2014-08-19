<!DOCTYPE html>
<!--[if IE 8]>         <html class="ie8"> <![endif]-->
<!--[if IE 9]>         <html class="ie9 gt-ie8"> <![endif]-->
<!--[if gt IE 9]><!--> <html class="gt-ie8 gt-ie9 not-ie"> <!--<![endif]-->

	<g:render template="/layouts/layoutHead"/>
	
    <body class="${pageProperty( name:'body.theme' ) ?: 'selected-theme'} ${pageProperty( name:'body.class' )}">

		<div id="main-wrapper">

			<g:render template="/layouts/topBanner"/>
	
	        <g:layoutBody />
        
        </div>
                
		<r:layoutResources/>
    </body>
</html>