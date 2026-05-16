function extractTableId(options = {}) {
  if (options.tableId) {
    return Number(options.tableId)
  }

  if (!options.scene) {
    return null
  }

  const decoded = decodeURIComponent(options.scene)
  const directId = decoded.match(/^\d+$/)
  if (directId) {
    return Number(directId[0])
  }

  const namedId = decoded.match(/(?:^|&)tableId=(\d+)/i)
  if (namedId) {
    return Number(namedId[1])
  }

  return null
}

module.exports = {
  extractTableId
}
