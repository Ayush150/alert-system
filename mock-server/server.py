import http.server
import socketserver
import json
import os

PORT = 8080
DATA_FILE = os.path.join(os.path.dirname(__file__), 'data', 'alerts.json')

class MockAlertsHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/api/v1/alerts':
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            
            try:
                with open(DATA_FILE, 'r') as f:
                    data = f.read()
                self.wfile.write(data.encode('utf-8'))
            except FileNotFoundError:
                self.wfile.write(json.dumps([]).encode('utf-8'))
                
        elif self.path == '/health':
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps({"status": "ok"}).encode('utf-8'))
            
        else:
            self.send_response(404)
            self.end_headers()

if __name__ == '__main__':
    with socketserver.TCPServer(("", PORT), MockAlertsHandler) as httpd:
        print(f"Mock server running at port {PORT}")
        print(f"Health check: http://127.0.0.1:{PORT}/health")
        print(f"Alerts endpoint: http://127.0.0.1:{PORT}/api/v1/alerts")
        httpd.serve_forever()
