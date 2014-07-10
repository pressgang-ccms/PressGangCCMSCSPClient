# csprocessor completion

_csprocessor()
{
	local cur prev commands options build_options command

	COMPREPLY=()
	cur=`_get_cword`

	commands='add-revision assemble build checkout create edit info list preview publish pull pull-snapshot push push-translation revisions search setup snapshot status sync-translation template validate'

	if [[ $COMP_CWORD -eq 1 ]] ; then
		if [[ "$cur" == -* ]]; then
			COMPREPLY=( $( compgen -W '--host --config --help --username' -- $cur ) )
		else
			COMPREPLY=( $( compgen -W "$commands" -- $cur ) )
		fi
	else
		prev=${COMP_WORDS[COMP_CWORD-1]}
		case $prev in
			--config)
				_filedir 'ini'
				return 0;
				;;
			@(--host|-H))
				COMPREPLY=( $( compgen -A hostname "$cur" ) )
				return 0;
				;;
            --zanata-server)
				COMPREPLY=( $( compgen -A hostname "$cur" ) )
				return 0;
				;;
            @(--output|-o))
				COMPREPLY=( $( compgen -df "$cur" ) )
				return 0;
				;;
            --format)
				COMPREPLY=( $( compgen -W "publican publican-po jdocbook" -- "$cur" ) )
				return 0;
				;;
            --override)
				COMPREPLY=( $( compgen -W "Author_Group.xml= Revision_History.xml= Feedback.xml= revnumber= brand= pubsnumber=" "$cur" ) )
                compopt -o nospace
				return 0;
				;;
		esac

		command=${COMP_WORDS[1]}

        build_options='--hide-errors --show-contentspec --hide-injections --override --publican.cfg-override --injection-types --hide-bug-links --old-bug-links --force-bug-links --output --editor-links --lang --target-lang --show-report --fetch-pubsnum --revision --latest --draft --show-remarks --rev-message --flatten-topics --flatten --yes --format --skip-bug-link-validation --suggest-chunk-depth --fail-on-build-error --fail-on-build-warning --disable-ssl-cert --zanata-server --zanata-project --zanata-version'

		if [[ "$cur" == -* ]]; then
			# possible options for the command
			case $command in
				add-revision)
					options='--date --email --firstname --rev-message --revnumber --surname'
					;;
                assemble)
                    options="$build_options --no-build --hide-output --no-publican-build --publican-config"
                    ;;
                build)
                    options=$build_options
                    ;;
                checkout)
                    options='--force --zanata-server --zanata-project --zanata-version'
                    ;;
                create)
                    options='--no-csprocessor-cfg --force --message --rev-history --strict-titles'
                    ;;
                edit)
                    options='--content-spec --topic --rev-history --lang'
                    ;;
                list)
                    options='--limit'
                    ;;
                preview)
                    options="$build_options --no-build --hide-output --no-publican-build --publican-config --no-assemble"
                    ;;
                publish)
                    options="$build_options --no-build --hide-output --no-publican-build --publican-config --no-assemble --pub-message"
                    ;;
                pull)
                    options='--content-spec --topic --revision --output'
                    ;;
                pull-snapshot)
                    options='--revision --max-topic-revision --output --latest'
                    ;;
                push)
                    options='--push-only --message --rev-history --strict-titles'
                    ;;
                push-translation)
                    options='--zanata-server --zanata-project --zanata-version --disable-copytrans --disable-ssl-cert --yes --unfrozen-push --contentspec-only'
                    ;;
                revisions)
                    options='--content-spec --topic'
                    ;;
                snapshot)
                    options='--max-topic-revision --latest --new --message --rev-history'
                    ;;
                sync-translation)
                    options='--langs --zanata-server --zanata-project --zanata-version --disable-ssl-cert'
                    ;;
                template)
                    options='--commented --output'
                    ;;
                validate)
                    options='--strict-titles'
                    ;;
			esac
			options="$options --host --config --help"
			COMPREPLY=( $( compgen -W "$options" -- $cur ) )
		else
			if [[ "$command" == @(--help) ]]; then
				COMPREPLY=( $( compgen -W "$commands" -- $cur ) )
			else
				_filedir
			fi
		fi
	fi

	return 0
}
complete -F _csprocessor csprocessor
