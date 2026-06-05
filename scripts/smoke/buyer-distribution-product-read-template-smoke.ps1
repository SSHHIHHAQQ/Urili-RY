param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [Parameter(Mandatory = $true)]
    [string]$BuyerUsername,
    [string]$BuyerPassword = "U12346",
    [long]$SampleSpuId = 0,
    [int]$PageSize = 10,
    [long]$InvisibleSpuId = 0,
    [long]$MissingSpuId = 999999999
)

$ErrorActionPreference = "Stop"
$script:NormalizedBaseUrl = $BaseUrl.TrimEnd("/")
$script:ForbiddenFields = @(
    "sellerId", "sellerNo", "sellerName", "sellerSpuCode", "sellerSkuCode",
    "subjectId", "accountId", "terminal",
    "systemSpuCode", "systemSkuCode",
    "sourceType", "sourceRefType", "sourceRefId",
    "supplyPrice", "supplyPriceMin", "supplyPriceMax",
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

function Assert-BusinessCode {
    param(
        $Response,
        [int]$ExpectedCode,
        [string]$Label
    )

    $code = $Response.code
    $msg = $Response.msg
    Assert-True ($null -ne $Response) "$Label returned empty response."
    Assert-True ([int]$code -eq $ExpectedCode) "$Label expected code=$ExpectedCode, actual code=$code, msg=$msg"
}

function Assert-ContainsPermission {
    param(
        $Info,
        [string]$Permission
    )

    $permissions = @($Info.data.permissions)
    Assert-True ($permissions -contains $Permission) "buyer getInfo did not include permission '$Permission'."
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

function Login-Buyer {
    param(
        [string]$Username,
        [string]$Password
    )

    $response = Invoke-JsonRequest -Method "POST" -Path "/buyer/login" -Body @{
        username = $Username
        password = $Password
    }
    Assert-BusinessSuccess -Response $response -Label "buyer login"
    Assert-True ($response.data.terminal -eq "buyer") "buyer login returned terminal '$($response.data.terminal)'."
    Assert-True (-not [string]::IsNullOrWhiteSpace($response.data.token)) "buyer login did not return a token."
    Assert-True ($null -ne $response.data.subjectId) "buyer login did not return subjectId."
    Assert-True ($null -ne $response.data.accountId) "buyer login did not return accountId."
    Assert-True ($response.data.username -eq $Username) "buyer login returned username '$($response.data.username)'."

    Write-Host "[ok] buyer login: subjectId=$($response.data.subjectId), accountId=$($response.data.accountId)"
    return [pscustomobject]@{
        Token = $response.data.token
        SubjectId = [long]$response.data.subjectId
        AccountId = [long]$response.data.accountId
    }
}

function Logout-Buyer {
    param(
        [string]$Token
    )

    if ([string]::IsNullOrWhiteSpace($Token)) {
        return
    }

    try {
        $null = Invoke-JsonRequest -Method "POST" -Path "/buyer/logout" -Token $Token
        Write-Host "[ok] buyer logout cleanup completed."
    }
    catch {
        Write-Warning "buyer logout cleanup skipped: $($_.Exception.Message)"
    }
}

$buyerLogin = $null

try {
    Write-Host "[info] smoke target: $script:NormalizedBaseUrl"
    $anonymousList = Invoke-JsonRequest -Method "GET" -Path "/buyer/product/distribution-products/list?pageNum=1&pageSize=1"
    Assert-BusinessCode -Response $anonymousList -ExpectedCode 401 -Label "buyer product anonymous list"
    Write-Host "[ok] buyer product anonymous list denied."

    $buyerLogin = Login-Buyer -Username $BuyerUsername -Password $BuyerPassword

    $info = Invoke-JsonRequest -Method "GET" -Path "/buyer/getInfo" -Token $buyerLogin.Token
    Assert-BusinessSuccess -Response $info -Label "buyer getInfo"
    Assert-True ($info.data.terminal -eq "buyer") "buyer getInfo returned terminal '$($info.data.terminal)'."
    Assert-ContainsPermission -Info $info -Permission "buyer:product:distribution:list"
    Assert-ContainsPermission -Info $info -Permission "buyer:product:distribution:query"
    Write-Host "[ok] buyer getInfo includes distribution product permissions."

    $list = Invoke-JsonRequest -Method "GET" -Path "/buyer/product/distribution-products/list?pageNum=1&pageSize=$PageSize" -Token $buyerLogin.Token
    Assert-BusinessSuccess -Response $list -Label "buyer product list"
    Assert-NoForbiddenFields -Response $list -Label "buyer product list"
    Assert-True ($null -ne $list.rows) "buyer product list returned no rows property."

    $rows = @($list.rows)
    Assert-True ($rows.Count -gt 0 -and $null -ne $rows[0]) "buyer product list is empty; seed at least one ON_SALE product with ON_SALE SKU."
    foreach ($row in $rows) {
        Assert-True ($row.spuStatus -eq "ON_SALE") "buyer product list returned non-ON_SALE SPU status '$($row.spuStatus)'."
        Assert-True ($null -ne $row.salePriceMin) "buyer product list returned row without salePriceMin."
        Assert-True ($null -ne $row.salePriceMax) "buyer product list returned row without salePriceMax."
    }
    Write-Host "[ok] buyer product list: total=$($list.total), rows=$($rows.Count)"

    $forgedListPath = "/buyer/product/distribution-products/list?pageNum=1&pageSize=$PageSize&buyerId=999999&subjectId=999999&accountId=999999&terminal=seller&sellerId=999999&systemSpuCode=SHOULD_NOT_SCOPE&systemSkuCode=SHOULD_NOT_SCOPE&sellerSpuCode=SHOULD_NOT_SCOPE&sellerSkuCode=SHOULD_NOT_SCOPE&sourceType=SHOULD_NOT_SCOPE&spuStatus=DRAFT"
    $forgedList = Invoke-JsonRequest -Method "GET" -Path $forgedListPath -Token $buyerLogin.Token
    Assert-BusinessSuccess -Response $forgedList -Label "buyer product forged-scope list"
    Assert-NoForbiddenFields -Response $forgedList -Label "buyer product forged-scope list"
    $forgedRows = @($forgedList.rows)
    Assert-True ([long]$forgedList.total -eq [long]$list.total) "forged-scope list changed total from $($list.total) to $($forgedList.total)."
    Assert-True ($forgedRows.Count -eq $rows.Count) "forged-scope list changed row count from $($rows.Count) to $($forgedRows.Count)."
    if ($rows.Count -gt 0 -and $forgedRows.Count -gt 0) {
        Assert-True ([long]$forgedRows[0].spuId -eq [long]$rows[0].spuId) "forged-scope list changed first spuId from $($rows[0].spuId) to $($forgedRows[0].spuId)."
    }
    Write-Host "[ok] buyer product forged-scope list ignored client scope parameters."

    $spuId = $SampleSpuId
    if ($spuId -le 0) {
        $spuId = [long]$rows[0].spuId
    }
    Assert-True ($spuId -gt 0) "Unable to resolve sample spuId from product list."

    $detail = Invoke-JsonRequest -Method "GET" -Path "/buyer/product/distribution-products/$spuId" -Token $buyerLogin.Token
    Assert-BusinessSuccess -Response $detail -Label "buyer product detail"
    Assert-NoForbiddenFields -Response $detail -Label "buyer product detail"
    Assert-True ($null -ne $detail.data -and [long]$detail.data.spuId -eq $spuId) "buyer product detail did not return sample spuId=$spuId."
    Assert-True ($detail.data.spuStatus -eq "ON_SALE") "buyer product detail returned non-ON_SALE SPU status '$($detail.data.spuStatus)'."
    $detailSkus = @($detail.data.skus)
    Assert-True ($detailSkus.Count -gt 0 -and $null -ne $detailSkus[0]) "buyer product detail returned no visible skus for sample spuId=$spuId."
    foreach ($sku in $detailSkus) {
        Assert-True ($sku.skuStatus -eq "ON_SALE") "buyer product detail returned non-ON_SALE SKU status '$($sku.skuStatus)'."
    }
    Write-Host "[ok] buyer product detail: spuId=$spuId"

    $skus = Invoke-JsonRequest -Method "GET" -Path "/buyer/product/distribution-products/$spuId/skus" -Token $buyerLogin.Token
    Assert-BusinessSuccess -Response $skus -Label "buyer product skus"
    Assert-NoForbiddenFields -Response $skus -Label "buyer product skus"
    $skuRows = @($skus.data)
    Assert-True ($skuRows.Count -gt 0 -and $null -ne $skuRows[0]) "buyer product skus is empty for sample spuId=$spuId."
    foreach ($sku in $skuRows) {
        Assert-True ([long]$sku.spuId -eq $spuId) "buyer product skus returned skuId=$($sku.skuId) with spuId=$($sku.spuId), expected $spuId."
        Assert-True ($sku.skuStatus -eq "ON_SALE") "buyer product skus returned non-ON_SALE SKU status '$($sku.skuStatus)'."
    }
    Write-Host "[ok] buyer product skus: spuId=$spuId, rows=$($skuRows.Count)"

    if ($InvisibleSpuId -gt 0) {
        $invisibleDetail = Invoke-JsonRequest -Method "GET" -Path "/buyer/product/distribution-products/$InvisibleSpuId" -Token $buyerLogin.Token
        Assert-ProductDenied -Response $invisibleDetail -Label "buyer invisible product detail" -SpuId $InvisibleSpuId
        Write-Host "[ok] buyer invisible product detail denied: code=$($invisibleDetail.code), msg=$($invisibleDetail.msg)"

        $invisibleSkus = Invoke-JsonRequest -Method "GET" -Path "/buyer/product/distribution-products/$InvisibleSpuId/skus" -Token $buyerLogin.Token
        Assert-ProductDenied -Response $invisibleSkus -Label "buyer invisible product skus" -SpuId $InvisibleSpuId
        Write-Host "[ok] buyer invisible product skus denied: code=$($invisibleSkus.code), msg=$($invisibleSkus.msg)"
    }
    else {
        Write-Warning "InvisibleSpuId not provided; invisible product denial check skipped."
    }

    if ($MissingSpuId -gt 0) {
        $missingDetail = Invoke-JsonRequest -Method "GET" -Path "/buyer/product/distribution-products/$MissingSpuId" -Token $buyerLogin.Token
        Assert-ProductDenied -Response $missingDetail -Label "buyer missing product detail" -SpuId $MissingSpuId
        Write-Host "[ok] buyer missing product detail denied: code=$($missingDetail.code), msg=$($missingDetail.msg)"

        $missingSkus = Invoke-JsonRequest -Method "GET" -Path "/buyer/product/distribution-products/$MissingSpuId/skus" -Token $buyerLogin.Token
        Assert-ProductDenied -Response $missingSkus -Label "buyer missing product skus" -SpuId $MissingSpuId
        Write-Host "[ok] buyer missing product skus denied: code=$($missingSkus.code), msg=$($missingSkus.msg)"
    }

    Logout-Buyer -Token $buyerLogin.Token
    $loggedOutInfo = Invoke-JsonRequest -Method "GET" -Path "/buyer/getInfo" -Token $buyerLogin.Token
    Assert-BusinessCode -Response $loggedOutInfo -ExpectedCode 401 -Label "buyer getInfo after logout"
    $buyerLogin = $null
    Write-Host "[ok] buyer logout invalidated token."

    Write-Host "[pass] buyer distribution product smoke completed."
}
finally {
    if ($null -ne $buyerLogin) {
        Logout-Buyer -Token $buyerLogin.Token
    }
}
