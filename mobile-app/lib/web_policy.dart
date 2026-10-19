/// Only the configured deployment owns the authenticated session.
Uri? deploymentUrl(String value) {
  final uri = Uri.tryParse(value.trim());
  if (uri == null || uri.host.isEmpty || uri.userInfo.isNotEmpty) return null;
  final local = {'localhost', '127.0.0.1', '10.0.2.2'}.contains(uri.host);
  if (uri.scheme != 'https' && !(local && uri.scheme == 'http')) return null;
  return uri.replace(fragment: '');
}

bool sameDeployment(Uri deployment, Uri target) =>
    deployment.scheme == target.scheme &&
    deployment.host == target.host &&
    deployment.port == target.port;

bool allowedExport(String name, int bytes) =>
    {'physlive.json', 'physlive.csv', 'physlive-replay.html'}.contains(name) &&
    bytes <= 50 * 1024 * 1024;
