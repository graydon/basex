(:~
 : This module contains some basic examples for RESTXQ annotations.
 : @author BaseX Team
 :)
module namespace page = 'http://basex.org/modules/web-page';

(:~
 : Generates a welcome page.
 : @return HTML page
 :)
declare
  %rest:path("")
  %output:method("xhtml")
  %output:omit-xml-declaration("no")
  %output:doctype-public("-//W3C//DTD XHTML 1.0 Transitional//EN")
  %output:doctype-system("http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd")
function page:start(
) as element(Q{http://www.w3.org/1999/xhtml}html) {
  <html xmlns="http://www.w3.org/1999/xhtml">
    <head>
      <title>BaseX HTTP Services</title>
      <link rel="stylesheet" type="text/css" href="static/style.css"/>
    </head>
    <body>
      <div class="right"><img src="static/basex.svg" width="96"/></div>
      <h2>BaseX HTTP Services</h2>
      <div>Welcome to the BaseX HTTP Services. They allow you to:</div>
      <ul>
        <li>Query and modify databases via <a href="http://docs.basex.org/wiki/REST">REST</a> (try <a href='rest'>here</a>),</li>
        <li>Browse and update resources via <a href="http://docs.basex.org/wiki/WebDAV">WebDAV</a>, and</li>
        <li>Create web applications and services with <a href="http://docs.basex.org/wiki/RESTXQ">RESTXQ</a></li>
      </ul>

      <p>Single services can be deactivated by modifying the <code>web.xml</code> file.</p>

      <p>The <a href="dba/">Database Administration</a> interface (DBA) is an
      example for a full RESTXQ web application.<br/>
      Both DBA and the following examples may help you to create your own web sites:</p>

      <h3>Example 1</h3>
      <p>The following links return different results.
      Both are generated by the same RESTXQ function:</p>
      <ul>
        <li><a href="hello/World">/hello/World</a></li>
        <li><a href="hello/Universe">/hello/Universe</a></li>
      </ul>

      <h3>Example 2</h3>
      <p>The next example presents how form data is processed via RESTXQ and the POST method:</p>
      <form method="post" action="form">
        <p>Your message:<br />
        <input name="message" size="50"></input>
        <input type="submit" /></p>
      </form>

      <h3>Example 3</h3>
      <p>The source code of the file that created this page
      (<code>{ static-base-uri() }</code>) is shown below:</p>
      <pre>{ unparsed-text(static-base-uri()) }</pre>
    </body>
  </html>
};

(:~
 : Returns an XML response message.
 : @param  $world  string to be included in the response
 : @return response element 
 :)
declare
  %rest:path("/hello/{$world}")
  %rest:GET
function page:hello(
  $world as xs:string
) as element(response) {
  <response>
    <title>Hello { $world }!</title>
    <time>The current time is: { current-time() }</time>
  </response>
};

(:~
 : Returns the result of a form request.
 : @param  $message  message to be included in the response
 : @param  $agent    user agent string
 : @return response element 
 :)
declare
  %rest:path("/form")
  %rest:POST
  %rest:form-param("message","{$message}", "(no message)")
  %rest:header-param("User-Agent", "{$agent}")
function page:hello-postman(
  $message as xs:string,
  $agent   as xs:string*

) as element(response) {
  <response type='form'>
    <message>{ $message }</message>
    <user-agent>{ $agent }</user-agent>
  </response>
};
