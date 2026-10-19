import 'package:flutter_test/flutter_test.dart';
import 'package:physlive_mobile/web_policy.dart';

void main() {
  test('deployment requires HTTPS and rejects embedded credentials', () {
    expect(deploymentUrl('https://school.example/app'), isNotNull);
    expect(deploymentUrl('http://school.example'), isNull);
    expect(deploymentUrl('https://user:password@school.example'), isNull);
    expect(deploymentUrl('javascript:alert(1)'), isNull);
    expect(deploymentUrl('http://10.0.2.2:5173'), isNotNull);
  });
  test('authenticated navigation stays on the configured origin', () {
    final site = Uri.parse('https://school.example');
    expect(
      sameDeployment(site, Uri.parse('https://school.example/workspace')),
      isTrue,
    );
    expect(
      sameDeployment(site, Uri.parse('https://school.example.attacker.test')),
      isFalse,
    );
    expect(sameDeployment(site, Uri.parse('http://school.example')), isFalse);
    expect(
      sameDeployment(site, Uri.parse('https://school.example:8443')),
      isFalse,
    );
  });
  test('exports cannot write arbitrary paths or unlimited files', () {
    expect(allowedExport('physlive-replay.html', 2048), isTrue);
    expect(allowedExport('../secret', 10), isFalse);
    expect(allowedExport('physlive.json', 51 * 1024 * 1024), isFalse);
  });
}
