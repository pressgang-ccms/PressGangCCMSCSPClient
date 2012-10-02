Name: csp-repo
Summary: Content Specification Processor Repository
License: GPLv2
Vendor: Red Hat, Inc.
Group: Development/Tools
Version: 1
Release: 1
BuildRoot: %{_builddir}/%{name}-buildroot
Packager: Lee Newson
BuildArch: noarch
URL: http://sourceforge.net/p/csprocessor/
Source: csprocessor-repo.tar.gz

%description
The repository configuration file to eb able to download the client for the content specification processor.

%prep
%setup -q

%build

%install
rm -rf $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT%{_sysconfdir}/yum.repos.d
install -m 0644 csprocessor.repo $RPM_BUILD_ROOT%{_sysconfdir}/yum.repos.d/csprocessor.repo

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root,-)
%{_sysconfdir}/yum.repos.d/csprocessor.repo

%changelog
* Thu Feb 9 2012 lnewson
- Created initial spec file
