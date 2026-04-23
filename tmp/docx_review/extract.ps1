$xmlPath = 'tmp\docx_review\word\document.xml'
[xml]$xml = Get-Content $xmlPath
$ns = New-Object System.Xml.XmlNamespaceManager($xml.NameTable)
$ns.AddNamespace('w','http://schemas.openxmlformats.org/wordprocessingml/2006/main')
$body = $xml.document.body
$index = 0
foreach ($node in $body.ChildNodes) {
  if ($node.LocalName -eq 'p') {
    $texts = $node.SelectNodes('.//w:t',$ns) | ForEach-Object { $_.'#text' }
    $text = ($texts -join '')
    if ($text.Trim()) {
      $styleNode = $node.SelectSingleNode('./w:pPr/w:pStyle/@w:val',$ns)
      $style = if ($styleNode) { $styleNode.Value } else { '' }
      Write-Output ("P|$index|$style|$text")
      $index++
    }
  } elseif ($node.LocalName -eq 'tbl') {
    $rows = @()
    foreach ($tr in $node.SelectNodes('./w:tr',$ns)) {
      $cells = @()
      foreach ($tc in $tr.SelectNodes('./w:tc',$ns)) {
        $cellTexts = $tc.SelectNodes('.//w:t',$ns) | ForEach-Object { $_.'#text' }
        $cells += (($cellTexts -join '') -replace '\s+', ' ').Trim()
      }
      if ($cells.Count -gt 0) { $rows += ('[' + ($cells -join ' | ') + ']') }
    }
    if ($rows.Count -gt 0) {
      Write-Output ("T|$index||" + ($rows -join ' || '))
      $index++
    }
  }
}
