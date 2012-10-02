Name: cspclient
Summary: Content Specification Processor client application
License: GPLv2
Vendor: Red Hat, Inc.
Group: Development/Tools
Version: 0.16.1
Release: 1
BuildRoot: %{_builddir}/%{name}-buildroot
Packager: Lee Newson
BuildArch: noarch
URL: http://sourceforge.net/p/csprocessor/
Requires: java-1.6.0-openjdk
Source: %{name}-%{version}.tar.gz

%description
A basic java application that allows a user to connect and work with the Content Specification Processor.

%prep
%setup -q

%build
(echo \#\!/bin/bash; echo ""; echo "java -jar %{_libdir}/CSPClient/skynet.jar \"\$@\"") > skynet.sh

%install
rm -rf $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT%{_bindir}
install -m 0755 -d $RPM_BUILD_ROOT%{_libdir}/CSPClient
install -m 0755 skynet.jar $RPM_BUILD_ROOT%{_libdir}/CSPClient/skynet.jar
install -m 0755 skynet.sh $RPM_BUILD_ROOT%{_bindir}/skynet

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root,-)
%dir %{_libdir}/CSPClient
%{_libdir}/CSPClient/skynet.jar
%{_bindir}/skynet

%postun

%changelog
* Tue Feb 7 2012 lnewson
- Updated the client to use the latest build 0.16.1 (goes with server build 0.20.1)
* Thu Feb 2 2012 lnewson
- Updated the help files to go with the new deployment strategy
* Thu Feb 2 2012 lnewson
- Created initial spec file
