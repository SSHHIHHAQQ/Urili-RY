param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [Parameter(Mandatory = $true)]
    [string]$SellerUsername,
    [string]$SellerPassword = "U12346",
    [string]$OtherSellerUsername,
    [string]$OtherSellerPassword = "U12346",
    [long]$SampleSpuId = 0,
    [int]$PageSize = 10
)

$ErrorActionPreference = "Stop"
$script:NormalizedBaseUrl = $BaseUrl.TrimEnd("/")
$script:ForbiddenFields = @(
    "sellerId", "subjectId", "accountId", "terminal",
    "systemSpuCode", "systemSkuCode", "sellerNo", "sellerName",
    "sourceType", "sourceRefType", "sourceRefId",
    "createBy", "createTime", "updateBy", "updateTime", "remark",
    "password", "token", "tokenId", "redisKey"
)
$script:NotFoundMessage = [string]::Concat([char[]](0x5546, 0x57CE, 0x5546, 0x54C1, 0x4E0D, 0x5B58, 0x5728))

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Invoke-JsonRequest {
    param(
        [ValidateSet("GET", "POST")]
        [string]$Method,
        [string]$Path,
        [string]$Token,
        $Body
    )

    $headers = @{}
    if (-not [string]::IsNullOrWhiteSpace($Token)) {
        $headers["Authorization"] = "Bearer $Token"
    }

    $uri = "$script:NormalizedBaseUrl$Path"
    try {
        if ($null -ne $Body) {
            $jsonBody = $Body | ConvertTo-Json -Depth 20
            return Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers -ContentType "application/json" -Body $jsonBody
        }
        return Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers
    }
    catch {
        $status = "unknown"
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            $status = [int]$_.Exception.Response.StatusCode
        }
        throw "HTTP $Method $Path failed, status=$status"
    }
}

function Assert-BusinessSuccess {
    param(
        $Response,
        [string]$Label
    )

    $code = $Response.code
    $msg = $Response.msg
    Assert-True ($null -ne $Response) "$Label returned empty response."
    Assert-True ([int]$code -eq 200) "$Label failed, code=$code, msg=$msg"
}

function Test-PropertyNameExists {
    param(
        $Value,
        [string]$PropertyName
    )

    if ($null -eq $Value) {
        return $false
    }

    if ($Value -is [string] -or $Value.GetType().IsPrimitive) {
        return $false
    }

    if ($Value -is [System.Collections.IDictionary]) {
        foreach ($key in $Value.Keys) {
            if ([string]$key -eq $PropertyName) {
                return $true
            }
            if (Test-PropertyNameExists -Value $Value[$key] -PropertyName $PropertyName) {
                return $true
            }
        }
        return $false
    }

    if ($Value -is [pscustomobject]) {
        foreach ($property in $Value.PSObject.Properties) {
            if ($property.Name -eq $PropertyName) {
                return $true
            }
            if (Test-PropertyNameExists -Value $property.Value -PropertyName $PropertyName) {
                return $true
            }
        }
        return $false
    }

    if ($Value -is [System.Collections.IEnumerable]) {
        foreach ($item in $Value) {
            if (Test-PropertyNameExists -Value $item -PropertyName $PropertyName) {
                return $true
            }
        }
    }

    return $false
}

function Assert-NoForbiddenFields {
    param(
        $Response,
        [string]$Label
    )

    foreach ($field in $script:ForbiddenFields) {
        Assert-True (-not (Test-PropertyNameExists -Value $Response -PropertyName $field)) "$Label leaked forbidden field '$field'."
    }
}

function Convert-MojibakeUtf8Text {
    param(
        [string]$Text
    )

    if ([string]::IsNullOrEmpty($Text)) {
        return $Text
    }

    $bytes = New-Object byte[] $Text.Length
    for ($index = 0; $index -lt $Text.Length; $index++) {
        $codePoint = [int][char]$Text[$index]
        if ($codePoint -gt 255) {
            return $Text
        }
        $bytes[$index] = [byte]$codePoint
    }

    try {
        return [System.Text.Encoding]::UTF8.GetString($bytes)
    }
    catch {
        return $Text
    }
}

function Assert-ProductDenied {
    param(
        $Response,
        [string]$Label,
        [long]$SpuId
    )

    Assert-True ([int]$Response.code -eq 500) "$Label unexpectedly returned code=$($Response.code) for spuId=$SpuId."
    $normalizedMsg = Convert-MojibakeUtf8Text -Text $Response.msg
    Assert-True ($normalizedMsg -eq $script:NotFoundMessage) "$Label returned unexpected msg '$($Response.msg)' for spuId=$SpuId."
}

function Login-Seller {
    param(
        [string]$Username,
        [string]$Password,
        [string]$Label
    )

    $response = Invoke-JsonRequest -Method "POST" -Path "/seller/login" -Body @{
        username = $Username
        password = $Password
    }
    Assert-BusinessSuccess -Response $response -Label "$Label login"
    Assert-True ($response.data.terminal -eq "seller") "$Label login returned terminal '$($response.data.terminal)'."
    Assert-True (-not [string]::IsNullOrWhiteSpace($response.data.token)) "$Label login did not return a token."
    Assert-True ($null -ne $response.data.subjectId) "$Label login did not return subjectId."
    Assert-True ($null -ne $response.data.accountId) "$Label login did not return accountId."
    Assert-True ($response.data.username -eq $Username) "$Label login returned username '$($response.data.username)'."

    Write-Host "[ok] $Label login: subjectId=$($response.data.subjectId), accountId=$($response.data.accountId)"
    return [pscustomobject]@{
        Token = $response.data.token
        SubjectId = [long]$response.data.subjectId
        AccountId = [long]$response.data.accountId
    }
}

function Logout-Seller {
    param(
        [string]$Token,
        [string]$Label
    )

    if ([string]::IsNullOrWhiteSpace($Token)) {
        return
    }

    try {
        $null = Invoke-JsonRequest -Method "POST" -Path "/seller/logout" -Token $Token
        Write-Host "[ok] $Label logout cleanup completed."
    }
    catch {
        Write-Warning "$Label logout cleanup skipped: $($_.Exception.Message)"
    }
}

$sellerLogin = $null
$otherSellerLogin = $null

try {
    Write-Host "[info] smoke target: $script:NormalizedBaseUrl"
    $sellerLogin = Login-Seller -Username $SellerUsername -Password $SellerPassword -Label "seller"

    $list = Invoke-JsonRequest -Method "GET" -Path "/seller/product/distribution-products/list?pageNum=1&pageSize=$PageSize" -Token $sellerLogin.Token
    Assert-BusinessSuccess -Response $list -Label "seller product list"
    Assert-NoForbiddenFields -Response $list -Label "seller product list"
    Assert-True ($null -ne $list.rows) "seller product list returned no rows property."

    $rows = @($list.rows)
    Assert-True ($rows.Count -gt 0 -and $null -ne $rows[0]) "seller product list is empty; seed at least one distribution product for this seller."
    Write-Host "[ok] seller product list: total=$($list.total), rows=$($rows.Count)"

    $forgedListPath = "/seller/product/distribution-products/list?pageNum=1&pageSize=$PageSize&sellerId=999999&subjectId=999999&accountId=999999&terminal=buyer&systemSpuCode=SHOULD_NOT_SCOPE&sourceType=SHOULD_NOT_SCOPE"
    $forgedList = Invoke-JsonRequest -Method "GET" -Path $forgedListPath -Token $sellerLogin.Token
    Assert-BusinessSuccess -Response $forgedList -Label "seller product forged-scope list"
    Assert-NoForbiddenFields -Response $forgedList -Label "seller product forged-scope list"
    $forgedRows = @($forgedList.rows)
    Assert-True ([long]$forgedList.total -eq [long]$list.total) "forged-scope list changed total from $($list.total) to $($forgedList.total)."
    Assert-True ($forgedRows.Count -eq $rows.Count) "forged-scope list changed row count from $($rows.Count) to $($forgedRows.Count)."
    if ($rows.Count -gt 0 -and $forgedRows.Count -gt 0) {
        Assert-True ([long]$forgedRows[0].spuId -eq [long]$rows[0].spuId) "forged-scope list changed first spuId from $($rows[0].spuId) to $($forgedRows[0].spuId)."
    }
    Write-Host "[ok] seller product forged-scope list ignored client scope parameters."

    $spuId = $SampleSpuId
    if ($spuId -le 0) {
        $spuId = [long]$rows[0].spuId
    }
    Assert-True ($spuId -gt 0) "Unable to resolve sample spuId from product list."

    $detail = Invoke-JsonRequest -Method "GET" -Path "/seller/product/distribution-products/$spuId" -Token $sellerLogin.Token
    Assert-BusinessSuccess -Response $detail -Label "seller product detail"
    Assert-NoForbiddenFields -Response $detail -Label "seller product detail"
    Assert-True ($null -ne $detail.data -and [long]$detail.data.spuId -eq $spuId) "seller product detail did not return sample spuId=$spuId."
    Write-Host "[ok] seller product detail: spuId=$spuId"

    $skus = Invoke-JsonRequest -Method "GET" -Path "/seller/product/distribution-products/$spuId/skus" -Token $sellerLogin.Token
    Assert-BusinessSuccess -Response $skus -Label "seller product skus"
    Assert-NoForbiddenFields -Response $skus -Label "seller product skus"
    $skuRows = @($skus.data)
    Assert-True ($skuRows.Count -gt 0 -and $null -ne $skuRows[0]) "seller product skus is empty for sample spuId=$spuId."
    foreach ($sku in $skuRows) {
        Assert-True ([long]$sku.spuId -eq $spuId) "seller product skus returned skuId=$($sku.skuId) with spuId=$($sku.spuId), expected $spuId."
    }
    Write-Host "[ok] seller product skus: spuId=$spuId, rows=$($skuRows.Count)"

    if (-not [string]::IsNullOrWhiteSpace($OtherSellerUsername)) {
        $otherSellerLogin = Login-Seller -Username $OtherSellerUsername -Password $OtherSellerPassword -Label "other seller"
        Assert-True ($otherSellerLogin.SubjectId -ne $sellerLogin.SubjectId) "Other seller account resolved to the same subjectId; cross-seller check is not meaningful."

        $otherDetail = Invoke-JsonRequest -Method "GET" -Path "/seller/product/distribution-products/$spuId" -Token $otherSellerLogin.Token
        Assert-ProductDenied -Response $otherDetail -Label "cross-seller product detail" -SpuId $spuId
        Write-Host "[ok] cross-seller product detail denied: code=$($otherDetail.code), msg=$($otherDetail.msg)"

        $otherSkus = Invoke-JsonRequest -Method "GET" -Path "/seller/product/distribution-products/$spuId/skus" -Token $otherSellerLogin.Token
        Assert-ProductDenied -Response $otherSkus -Label "cross-seller product skus" -SpuId $spuId
        Write-Host "[ok] cross-seller product skus denied: code=$($otherSkus.code), msg=$($otherSkus.msg)"
    }
    else {
        Write-Warning "OtherSellerUsername not provided; cross-seller denial check skipped."
    }

    Write-Host "[pass] seller distribution product smoke completed."
}
finally {
    if ($null -ne $otherSellerLogin) {
        Logout-Seller -Token $otherSellerLogin.Token -Label "other seller"
    }
    if ($null -ne $sellerLogin) {
        Logout-Seller -Token $sellerLogin.Token -Label "seller"
    }
}
