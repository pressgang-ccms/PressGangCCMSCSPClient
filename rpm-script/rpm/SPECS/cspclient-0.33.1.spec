Name: cspclient
Summary: Content Specification Processor client application
License: LGPL
Vendor: Red Hat, Inc.
Group: Development/Tools
Version: 0.33.1
Release: 1
BuildRoot: %{_builddir}/%{name}-buildroot
Packager: Lee Newson
BuildArch: noarch
URL: https://github.com/pressgang-ccms/PressGangCCMSCSPClient/
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
* Fri Jun 28 2013 lnewson - 0.33.1
- Fixed a NPE that could occur when including a Revision History topic
* Fri Jun 28 2013 lnewson - 0.33.0
- BZ #967678: Added tech-preview for building jDocbook compatible output.
- BZ #967677: Added the ability to build books on a "file-per-chapter" basis.
- BZ #966788: Added the ability to disable the assignee on bugzilla links.
- BZ #979208: Added a way to set the Bugzilla Keywords field.
- BZ #971638: Fixed the status command.
- BZ #968991: Fixed an issue to prevent HTTP timeouts when creating new topics with URL's.
- BZ #957995: Fixed an issue where building with an invalid locale doesn't fail.
- BZ #977587: Fixed Translated Revision Histories have default content inserted.
- BZ #977332: Fixed levels cannot define a related topic.
- Fixed an issue where server builds would set the Fixed URL for topics.
- Removed all old references to the cloud build output.
- Removed the dependency on the PressGang user table.
* Thu May 30 2013 lnewson - 0.32.3
- BZ #968197: Fixed a bug that was causing builds to fail when front matter injection was used along with injections.
- Fixed a bug so that the new server url's could be used.
- Fixed a bug that would cause the csprocessor to always use the default config.
* Wed May 23 2013 lnewson - 0.32.2
- Fixed a bug that was causing the program to crash when invalid XML entities were being used.
- Fixed a regression in parsing the metadata of a Content Spec.
* Tue May 22 2013 lnewson - 0.32.1
- Changed the Validator to allow empty levels where an intro topic is specified, except on Part and Section.
- Fixed a bug that was not allowing the default entities to be used which are required for legalnotices.
* Tue May 14 2013 lnewson
- Added RFEs: BZ #959807, BZ #913061, BZ #809802 & BZ #799924.
- Added the ability to push/create using log messages.
- Fixed a few other minor bugs.
* Tue Apr 30 2013 lnewson - 0.32.0
- Added the sync-translation command.
- Fixed a bug that was causing push-translation to use the wrong zanata server.
* Mon Apr 22 2013 lnewson
- Added a way to compress build output to speed up publican builds
* Tue Apr 16 2013 lnewson
- Fixed a bug that was causing "create" actions to fail
- Fixed a bug where assembling translations would fail under certain configs
- Fixed a bug that was causing publish commands to fail
* Fri Apr 12 2013 lnewson
- Added RFEs: BZ #923527 and BZ #861464
- Fixed Bugs: BZ #923524 and BZ #912959
- Fixed a bug when checking out content specs.
- Added the "Copyright Year" metadata for content specs.
* Tue Apr 02 2013 lnewson
- Fixed a bug where including some xml examples in XML would cause errors.
* Tue Mar 19 2013 lnewson
- Fixed a bug where pushing translations would report a failure when it actually succeeded.
- Changed the editor URL's to point to the new UI.
- Fixed a Bug where the URL's weren't being populated when using the default settings.
* Mon Mar 18 2013 lnewson
- Fixed a bug where the pubsnumber wasn't getting overriden.
- Fixed a bug caused by a change in the REST API.
* Fri Mar 15 2013 lnewson
- Fixed a bug in the builder that was causing builds to fail with the "--flatten-topics" option.
* Thu Mar 14 2013 lnewson
- Added RFE BZ #888103
- Added RFE BZ #885570
- Added RFE BZ #919300
- Fixed BZ #896291
- Fixed a bug where pushing translations would say it was successful when it wasn't.
- Fixed a bug that was causing the program to freeze after completing some commands.
- Fixed a bug where pulling snapshots would not preserve human readable relationships.
- Changed the report a bug links to a new format.
* Wed Dec 19 2012 lnewson
- Fixed an issue that would cause the builder to enter an infinite loop.
- Fixed Bug #888128
* Wed Dec 12 2012 lnewson
- Added Japanese Translations
* Mon Dec 10 2012 lnewson
- Fixed an issue with regards to an error that would stop fixed URL's from working.
* Thu Dec 06 2012 lnewson
- Fixed an issue where the book wouldn't build if the Fixed URL's couldn't be used.
* Wed Dec 05 2012 lnewson
- Fixed an issue where Fixed URL's weren't being set.
* Mon Nov 26 2012 lnewson
- Fixed an issue with building Articles
- Various minor fixes
- Fixed bug #878706
- Added a new parameter to specify the output translation locale
* Wed Nov 14 2012 lnewson
- Fixed a regression with Processes.
* Thu Nov 01 2012 lnewson
- Added the snapshot command.
- Added Translations for build output.
- Added an option to only push topics for translation.
- Various minor fixes/tweaks
* Wed Oct 17 2012 lnewson
- Fixed an issue in the 0.27.5 release that was causing books to fail publican validation
- Added code to handle long Zanata URLs.
* Tue Oct 16 2012 lnewson
- Fixed an issue with some books being unable to build.
- Fixed an issue with the Content Spec zanata ID not being included in the zanata link.
* Mon Oct 15 2012 lnewson
- Fixed a bug with building translations and revisions where the Fixed URL property was no longer unique.
- Fixed a bug where some Error Links were being picked up as invalid incorrectly.
- Fixed a bug where two different topics with the same name would result in the same Fixed URL.
- Fixed a bug that was causing the XML to be respresented incorrectly in some cases.
- Added code samples for more error messages.
- Updated the Zanata client library to 1.6.0.
* Wed Oct 10 2012 lnewson
- Fixed a bug that didn't create TranslatedTopics when using the push-translation command.
- Fixed a bug where the builder would only apply translations when the revision matched.
* Mon Oct 08 2012 lnewson
- Fixed a bug that was duplicating fixed URL's
- Fixed a bug that was adding incorrect data to Revision_History.xml
* Wed Oct 03 2012 mcasperson
- Fixed bug with fixed URL generation
* Thu Sep 20 2012 lnewson
- Added the ability to build Articles. See Bug #833290
- Made the CSP include keyword sets for topics. See Bug #836838
- Added the ability to build and create snapshots from a revision
- Added the ability to build drafts and to show remarks.
- Added more data in the "Report a Bug" links. See Bug #846196
- Added the ability to include topics in a Part. See Bug #824597
- Removed Pubsnumber options and processing.
- Added a "revnumber" override, to replace the default revision number.
- Fixed an issue with parsing relationships. See Bug #857203
- Fixed an issue where some relationship errors were ambiguous. See Bug #839464
- Fixed a bug that prevented translated books being built by Publican.
- Fixed a bug that prevented Punctuation in Content Spec Titles.
- Added ways to populate the Revision_History.xml using Metadata and Command line options.
- Fixed a bug that was naming images incorrectly.
- Other minor fixes.
* Thu Sep 06 2012 lnewson
- Fixed an bug with creating Fixed URL's in the builder.
* Wed Sep 05 2012 lnewson
- Fixed an issue with checkout and pull operations.
- Added the ability to properly build translated books with a link to edit the content specs.
- Fixes from other libraries to do with pushing translations.
* Sun Aug 19 2012 lnewson
- Added functionality to order Authors by Name, instead of having no ordering.
- Changed the way formatting XML elements are retreived.
- Fixed an issue with the push-translation command.
* Tue Aug 07 2012 lnewson
- Added the functionaility to specify condition statements when building
* Wed Aug 01 2012 lnewson
- Added the ability to pull Common_Content for a different locale (workaround for a publican bug).
- Fixed an issue where the Dummy Translated topics were showing new content.
- Added the ability to build translated books from snapshots.
* Thu Jul 26 2012 lnewson
- Fixed an issue where the Author_Group.xml wasn't getting populated
- Fixed an issue with push translations when multiple translations exist
* Wed Jul 25 2012 lnewson
- Fixed an issue with translation builds
- Added functionality to check links in topics
- Added default zanata values for project and version
- Added the Build Report functionality
- Fixed an issue with building and using the --output parameter
- Minor Bug Fixes
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
