Name: cspclient
Summary: Content Specification Processor client application
License: GPLv2
Vendor: Red Hat, Inc.
Group: Development/Tools
Version: 0.22.5
Release: 1
BuildRoot: %{_builddir}/%{name}-buildroot
Packager: Lee Newson
BuildArch: noarch
URL: http://sourceforge.net/p/csprocessor/
Requires: java-1.6.0-openjdk, publican
Source: %{name}-%{version}.tar.gz

%description
A basic java application that allows a user to connect and work with the Content Specification Processor.

%prep
%setup -q

%build
(echo \#\!/bin/bash; echo ""; echo "java -jar %{_libdir}/CSPClient/csprocessor.jar \"\$@\"") > csprocessor.sh

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
