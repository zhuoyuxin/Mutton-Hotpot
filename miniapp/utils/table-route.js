function normalizePositiveInt(value) {
  var num = Number(value)
  if (!Number.isInteger(num) || num <= 0) {
    return null
  }
  return num
}

function parseQueryString(query) {
  var normalized = String(query || '').replace(/^[?#]/, '')
  var result = {}

  if (!normalized) {
    return result
  }

  normalized.split('&').forEach(function (pair) {
    var parts
    var rawKey
    var rawValue
    var key
    var value

    if (!pair) {
      return
    }

    parts = pair.split('=')
    rawKey = parts[0]
    rawValue = parts.length > 1 ? parts[1] : ''

    if (!rawKey) {
      return
    }

    key = decodeURIComponent(rawKey)
    value = decodeURIComponent(rawValue)
    result[key] = value
  })

  return result
}

function extractTableIdFromScene(scene) {
  var decoded = decodeURIComponent(String(scene || ''))
  var directId = normalizePositiveInt(decoded)
  var query

  if (directId) {
    return directId
  }

  query = parseQueryString(decoded)
  return normalizePositiveInt(query.tableId)
}

function extractTableIdFromText(input) {
  var text = String(input || '').trim()
  var directId
  var namedId
  var sceneNamedId
  var decodedScene
  var fromScene
  var customerLoginPath
  var pageQueryIndex
  var query
  var queryId

  if (!text) {
    return null
  }

  directId = normalizePositiveInt(text)
  if (directId) {
    return directId
  }

  namedId = text.match(/(?:^|[?&#/])tableId=(\d+)/i)
  if (namedId) {
    return normalizePositiveInt(namedId[1])
  }

  sceneNamedId = text.match(/(?:^|[?&#])scene=([^&#]+)/i)
  if (sceneNamedId) {
    decodedScene = decodeURIComponent(sceneNamedId[1])
    fromScene = extractTableIdFromScene(decodedScene)
    if (fromScene) {
      return fromScene
    }
  }

  customerLoginPath = text.match(/\/c\/login\/(\d+)(?:[/?#]|$)/i)
  if (customerLoginPath) {
    return normalizePositiveInt(customerLoginPath[1])
  }

  pageQueryIndex = text.indexOf('?')
  if (pageQueryIndex > -1) {
    query = parseQueryString(text.slice(pageQueryIndex + 1))
    queryId = normalizePositiveInt(query.tableId)
    if (queryId) {
      return queryId
    }
  }

  return null
}

function extractTableId(options) {
  var safeOptions = options || {}
  var optionTableId = normalizePositiveInt(safeOptions.tableId)

  if (optionTableId) {
    return optionTableId
  }

  if (!safeOptions.scene) {
    return null
  }

  return extractTableIdFromScene(safeOptions.scene)
}

function extractTableIdFromScanResult(scanResult) {
  var fromPath

  if (!scanResult) {
    return null
  }

  if (typeof scanResult === 'string') {
    return extractTableIdFromText(scanResult)
  }

  fromPath = extractTableIdFromText(scanResult.path || '')
  if (fromPath) {
    return fromPath
  }

  return extractTableIdFromText(scanResult.result || '')
}

module.exports = {
  extractTableId: extractTableId,
  extractTableIdFromScanResult: extractTableIdFromScanResult
}
