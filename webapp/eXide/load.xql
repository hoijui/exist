(:
 :  eXist Open Source Native XML Database
 :  Copyright (C) 2011 The eXist Project
 :  http://exist-db.org
 :
 :  This program is free software; you can redistribute it and/or
 :  modify it under the terms of the GNU Lesser General Public License
 :  as published by the Free Software Foundation; either version 2
 :  of the License, or (at your option) any later version.
 :
 :  This program is distributed in the hope that it will be useful,
 :  but WITHOUT ANY WARRANTY; without even the implied warranty of
 :  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 :  GNU Lesser General Public License for more details.
 :
 :  You should have received a copy of the GNU Lesser General Public
 :  License along with this library; if not, write to the Free Software
 :  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 :
 :  $Id$
 :)
xquery version "1.0";

declare option exist:serialize "indent=yes";

let $path := request:get-parameter("path", ())
let $download := request:get-parameter("download", ())
let $mime := xmldb:get-mime-type($path)
let $log := util:log("INFO", ("MIME: ", $mime))
let $isBinary := util:is-binary-doc($path)
let $header := response:set-header("Content-Type", if ($mime) then $mime else "application/binary")
let $header2 :=
    if ($download) then
        response:set-header("Content-Disposition", concat("attachment; filename=", replace($path, "^.*/([^/]+)$", "$1")))
    else
        ()
return
    if ($isBinary) then
        let $data := util:binary-doc($path)
        return (
            response:stream-binary($data, $mime, ())
		)
    else (
        let $doc := doc($path)
        return
            if ($doc) then (
                $doc
            ) else
                response:set-status-code(404)
    )
