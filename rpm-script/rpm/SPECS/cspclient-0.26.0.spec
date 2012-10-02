Name: cspclient
Summary: Content Specification Processor client application
License: GPLv2
Vendor: Red Hat, Inc.
Group: Development/Tools
Version: 0.26.0
Release: 1
BuildRoot: %{_builddir}/%{name}-buildroot
Packager: Lee Newson
BuildArch: noarch
URL: http://sourceforge.net/p/csprocessor/
Requires: java >= 1:1.6.0, publican
Source: %{name}-%{version}.tar.gz

%description
A basic java application that allows a user to connect and work with the Content Specification Processor.

%prep
%setup -q

%build
(echo \#\!/bin/bash; echo ""; echo "java -Xmx1024m -Xms128m -jar %{_libdir}/CSPClient/csprocessor.jar \"\$@\"") > csprocessor.sh

%install
rm -rf $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT%{_bindir}
install -m 0755 -d $RPM_BUILD_ROOT%{_libdir}/CSPClient
install -m 0755 csprocessor.jar $RPM_BUILD_ROOT%{_libdir}/CSPClient/csprocessor.jar
install -m 0755 csprocessor.sh $RPM_BUILD_ROOT%{_bindir}/csprocessor

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root,-)
%dir %{_libdir}/CSPClient
%{_libdir}/CSPClient/csprocessor.jar
%{_bindir}/csprocessor

%changelog
* Wed Jul 18 2012 lnewson
- Added the publish and push-translation commands. Bugs #829947 & #834831 
- Added the ability to fetch the pubsnumber from brew. Bug #824693
- Fixed Bug #838102
- Fixed a few minor unreported bugs
* Thu Jul 12 2012 lnewson
- Fixes for bugs #815161, #837960 & #839129. Added the main part of the implementation for bugs #839460 & #839132.
- Changed how validation is done on tags to be more useful.
- Changed the validation process so that it stops if the syntax is invalid.
- Updated some error messages to be more descriptive.
* Sat Jul 09 2012 lnewson
- Fixed  Bug #831066
- Fixed an issue that would cause the client to crash when no default server was set
- Fixed an issue with the default translation settings
* Fri Jul 08 2012 lnewson
- Added assets for translated builds.
- Added the following RFEs: Bug #836082 & #836081
- Added a more human readable format for related to and prerequisite links
- Minor bug fixes
* Wed Jun 27 2012 lnewson
- Fixed an issue with processes and relationships
* Tue Jun 26 2012 lnewson
- Updated the csprocessor start-up script
- Fixed Bug #835288
- Fixed Bug #834387
- Fixed Bug #834539
- Fixed an issue where translated topics were being pulled from the REST API when they shouldn't have been.
* Tue Jun 19 2012 lnewson
- Added a command to create snapshots of content specs.
- Fixed an issue with pushing content specs with global tags.
- Added the remap attribute to built topics.
- Fixed a few internal issues to do with the REST API and caching.
* Thu Jun 07 2012 lnewson
- Fix for successful pushing saving to the wrong directory
- Fix for incorrect duplicate id processing
* Wed Jun 06 2012 lnewson
- Fixed an issue with processes
- Fixed an issue with image width
* Fri Jun 01 2012 lnewson
- Fixed an issue with the builder
- Fixed an issue with the assemble command
* Sun May 27 2012 lnewson
- Fixed an issue with empty fileref's on imagedata
- Fixed an issue with being unable to inject Survey Links
- Fixed an issue with bugzilla links
* Tue May 22 2012 lnewson
- Fixed an issue with an empty simplesect
* Mon May 21 2012 lnewson
- Version update with fixes and some requests
- Matches up with the 20120520-2353 skynet build
* Fri May 18 2012 lnewson
* Wed May 16 2012 lnewson
- Fixed some issues with invalid topic titles
- Fixed an issue with the builder where titles that were the same after being escaped caused duplicate id's
- Fixed an issue with the assemble and preview commands when a root directory was set
* Thu May 10 2012 lnewson
- Fixed an issue where CDATA elements were being removed
- Fixed an issue where CHECKSUMS wouldn't work with windows line endings
* Thu Apr 19 2012 lnewson
- Added extra params to the generated Bugzilla URL
- Updated the builder inline elements
* Tue Apr 17 2012 lnewson
- Fix the dependencies for java
* Mon Apr 16 2012 lnewson
- Updated to version 0.22.6
- Added the "csprocessor info" command
- Added the "part" element to Content Specifications
- Fixed a few bugs
* Mon Mar 26 2012 lnewson
- Updated to version 0.22.5
- Fixed an issue with the previous version and Java SE 6
* Mon Mar 26 2012 lnewson
- Updated to version 0.22.4
- Fixed an issue with the Status and Pull commands
- Changed the default for the configuration file
- Updated the builder to allow for more duplicates of Topic URL's
- Made <revnumber> an inline element to make it work with Publican 3
* Fri Mar 16 2012 lnewson
- Updated to version 0.22.3
- Fixed an issue with entities in the builder
- Fixed an issue from 0.22.2 with pulling specs
- Changed the output filename extension from .txt to .contentspec
* Wed Mar 14 2012 lnewson
- Updated to version 0.22.2
- Fixed an issue where certain commands wouldn't work if no root directory was specified.
* Thu Mar 8 2012 lnewson
- Fixed an issure with the program only working on Java SE7
* Mon Mar 5 2012 lnewson
- Updated the client to use the new client build 0.22.0
- Changed client since the CSP Server is going to close
* Mon Feb 20 2012 lnewson
- Updated the client to use the latest build 0.17.0
- Added the checksum commands
* Mon Feb 20 2012 lnewson
- Updated the client to use the latest build 0.16.4
- Fixed an issue with not being able to validate using permissive mode
* Thu Feb 16 2012 lnewson
- Updated the client to use the latest build 0.16.3
- Fixed a few minor bugs
* Tue Feb 7 2012 lnewson
- Updated the client to use the latest build 0.16.2 (goes with server build 0.20.1)
- Changed the filename and commands to csprocessor
* Tue Feb 7 2012 lnewson
- Updated the client to use the latest build 0.16.1 (goes with server build 0.20.1)
* Thu Feb 2 2012 lnewson
- Updated the help files to go with the new deployment strategy
* Thu Feb 2 2012 lnewson
- Created initial spec file
