{{/*
Generate a name for the application
*/}}
{{- define "msa-trading.name" -}}
msa-trading
{{- end }}

{{/*
Generate the full name including release name
*/}}
{{- define "msa-trading.fullname" -}}
{{ .Release.Name }}-msa-trading
{{- end }}
