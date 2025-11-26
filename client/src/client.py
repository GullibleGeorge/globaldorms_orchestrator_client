import requests
import json
from datetime import datetime

class GlobalDormClient:
    def __init__(self, base_url="http://localhost:8080"):
        """Initialize client session and check service availability"""
        self.base_url = base_url.rstrip('/')
        self.session = requests.Session()
        self.session.headers.update({
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        })
        
        # Test connection to orchestrator service
        if not self.test_connection():
            print("Warning: Could not connect to the Global Dorm service")
            print(f"Make sure the service is running at {base_url}")
    
    def test_connection(self):
        """Check if the orchestrator service is reachable"""
        try:
            response = self.session.get(f"{self.base_url}/api/health", timeout=5)
            return response.status_code == 200
        except requests.RequestException:
            return False
    
    def make_request(self, method, endpoint, data=None, params=None):
        """Generic HTTP request method with error handling"""
        try:
            url = f"{self.base_url}{endpoint}"
            
            if method.upper() == 'GET':
                response = self.session.get(url, params=params, timeout=10)
            elif method.upper() == 'POST':
                response = self.session.post(url, json=data, params=params, timeout=10)
            elif method.upper() == 'DELETE':
                response = self.session.delete(url, json=data, params=params, timeout=10)
            else:
                print(f"Unsupported HTTP method: {method}")
                return None
            
            if response.status_code == 200:
                return response.json()
            else:
                # Attempt to print server-provided error message
                error_data = response.json() if response.content else {}
                print(f"Request failed ({response.status_code}): {error_data.get('message', 'Unknown error')}")
                return None
                
        except requests.RequestException as e:
            print(f"Network error: {e}")
            return None
        except json.JSONDecodeError:
            print("Invalid JSON response from server")
            return None
    
    # ------------------------- Room-related methods -------------------------
    
    def search_rooms(self, filters=None):
        """Search for rooms using optional filters"""
        print("\nSearching for rooms...")
        response = self.make_request('GET', '/api/rooms', params=filters)
        
        if response and response.get('success'):
            rooms = response.get('rooms', [])
            total = response.get('total', 0)
            print(f"Found {total} room(s)")
            for room in rooms:
                self.display_room(room)
            return rooms
        else:
            print("Failed to search rooms")
            return []
    
    def get_room_details(self, room_id):
        """Retrieve detailed info for a specific room"""
        print(f"\nGetting details for room {room_id}...")
        response = self.make_request('GET', f'/api/rooms/{room_id}')
        if response and response.get('success'):
            room = response.get('room')
            self.display_room(room, detailed=True)
            return room
        else:
            print(f"Failed to get room {room_id} details")
            return None
    
    # ------------------------- Application methods -------------------------
    
    def apply_for_room(self, room_id, user_id, user_email):
        """Submit an application for a room"""
        print(f"\nApplying for room {room_id}...")
        data = {
            'room_id': room_id,
            'user_id': user_id,
            'user_email': user_email
        }
        response = self.make_request('POST', '/api/applications', data=data)
        if response and response.get('success'):
            print(f"SUCCESS: {response.get('message')}")
            print(f"Application ID: {response.get('application_id')}")
            return True
        else:
            print("Failed to apply for room")
            return False
    
    def cancel_application(self, application_id, user_id):
        """Cancel an existing room application"""
        print(f"\nCancelling application {application_id}...")
        data = {'user_id': user_id}
        response = self.make_request('DELETE', f'/api/applications/{application_id}', data=data)
        if response and response.get('success'):
            print(f"SUCCESS: {response.get('message')}")
            return True
        else:
            print("Failed to cancel application")
            return False
    
    def get_user_applications(self, user_id):
        """Retrieve all applications submitted by a user"""
        print(f"\nGetting applications for user {user_id}...")
        response = self.make_request('GET', f'/api/users/{user_id}/applications')
        if response and response.get('success'):
            applications = response.get('applications', [])
            total = response.get('total', 0)
            print(f"Found {total} application(s)")
            for app in applications:
                self.display_application(app)
            return applications
        else:
            print("Failed to get user applications")
            return []
    
    # ------------------------- External service methods -------------------------
    
    def get_distance_to_campus(self, room_postcode, campus_postcode):
        """Retrieve driving distance and duration from room to campus"""
        print(f"\nCalculating distance from {room_postcode} to {campus_postcode}...")
        params = {'room_postcode': room_postcode, 'campus_postcode': campus_postcode}
        response = self.make_request('GET', '/api/distance', params=params)
        if response and response.get('success'):
            distance_info = response.get('distance')
            print(f"Route: {distance_info['from']} to {distance_info['to']}")
            print(f"Distance: {distance_info['distance_miles']} miles ({distance_info['distance_km']} km)")
            print(f"Duration: {distance_info['duration_formatted']} ({distance_info['duration_minutes']} minutes)")
            return distance_info
        else:
            print("Failed to calculate distance")
            print("Tip: Make sure postcodes are valid UK postcodes (e.g., 'NG2 8PT')")
            return None
    
    def get_weather_forecast(self, postcode):
        """Retrieve weather forecast for a given postcode"""
        print(f"\nGetting weather forecast for {postcode}...")
        params = {'postcode': postcode}
        response = self.make_request('GET', '/api/weather', params=params)
        if response and response.get('success'):
            weather_info = response.get('weather')
            print(f"Weather forecast for {weather_info['location']}:")
            for day in weather_info['forecast']:
                print(f"Date {day['date']}: {day['weather']}")
                print(f"  Temperature: {day['temp_min']}-{day['temp_max']}°C")
                print(f"  Wind speed: {day['wind_speed']} km/h")
            return weather_info
        else:
            print("Failed to get weather forecast")
            return None
    
    # ------------------------- Display methods -------------------------
    
    def display_room(self, room, detailed=False):
        """Print room summary or detailed information"""
        print(f"\nRoom {room['id']}: {room['name']}")
        print(f"  Location: {room['location']['city']}, {room['location']['postcode']}")
        print(f"  Price: £{room['price_per_month_gbp']}/month")
        print(f"  Languages: {', '.join(room['spoken_languages'])}")
        if detailed:
            details = room['details']
            print(f"  Furnished: {'Yes' if details['furnished'] else 'No'}")
            print(f"  Shared with: {details['shared_with']} people")
            print(f"  Live-in landlord: {'Yes' if details['live_in_landlord'] else 'No'}")
            print(f"  Bills included: {'Yes' if details['bills_included'] else 'No'}")
            print(f"  Bathroom: {'Shared' if details['bathroom_shared'] else 'Private'}")
            print(f"  Amenities: {', '.join(details['amenities'])}")
            print(f"  Available from: {room['availability_date']}")
    
    def display_application(self, app):
        """Print a user's application summary"""
        print(f"\nApplication {app['id']} - Status: {app['status'].upper()}")
        print(f"  Room: {app['room_details']['name']}")
        print(f"  Location: {app['room_details']['location']['city']}")
        print(f"  Price: £{app['room_details']['price_per_month_gbp']}/month")
        print(f"  Applied on: {app['application_date'][:10]}")
        if app['status'] == 'cancelled' and 'cancelled_date' in app:
            print(f"  Cancelled on: {app['cancelled_date'][:10]}")

# ------------------------- Main CLI -------------------------

def main():
    """Main interactive menu for the Global Dorm client"""
    client = GlobalDormClient()
    
    print("Welcome to Global Dorm - Student Accommodation Finder")
    print("=" * 55)
    
    # Demo user credentials
    user_id = "student123"
    user_email = "student@example.com"
    
    while True:
        # Display menu
        print("\nMenu:")
        print("1. Search all rooms")
        print("2. Search rooms with filters")
        print("3. Get room details")
        print("4. Apply for a room")
        print("5. View my applications")
        print("6. Cancel an application")
        print("7. Check distance to campus")
        print("8. Get weather forecast")
        print("9. Exit")
        
        choice = input("\nEnter your choice (1-9): ").strip()
        
        # Map choices to functions
        if choice == '1':
            client.search_rooms()
        elif choice == '2':
            print("\nSearch Filters (press Enter to skip):")
            city = input("City: ").strip()
            max_price = input("Max price (£): ").strip()
            furnished = input("Furnished (true/false): ").strip()
            language = input("Language: ").strip()
            filters = {}
            if city: filters['city'] = city
            if max_price: filters['max_price'] = max_price
            if furnished: filters['furnished'] = furnished
            if language: filters['language'] = language
            client.search_rooms(filters)
        elif choice == '3':
            try:
                room_id = int(input("Enter room ID: ").strip())
                client.get_room_details(room_id)
            except ValueError:
                print("Invalid room ID")
        elif choice == '4':
            try:
                room_id = int(input("Enter room ID to apply for: ").strip())
                client.apply_for_room(room_id, user_id, user_email)
            except ValueError:
                print("Invalid room ID")
        elif choice == '5':
            client.get_user_applications(user_id)
        elif choice == '6':
            try:
                app_id = int(input("Enter application ID to cancel: ").strip())
                client.cancel_application(app_id, user_id)
            except ValueError:
                print("Invalid application ID")
        elif choice == '7':
            print("\nDistance Calculator (UK postcodes)")
            room_postcode = input("Enter room postcode: ").strip()
            campus_postcode = input("Enter campus postcode: ").strip()
            if room_postcode and campus_postcode:
                client.get_distance_to_campus(room_postcode, campus_postcode)
            else:
                print("Both postcodes are required")
        elif choice == '8':
            print("\nWeather Forecast (UK postcodes)")
            postcode = input("Enter postcode for weather forecast: ").strip()
            if postcode:
                client.get_weather_forecast(postcode)
            else:
                print("Postcode is required")
        elif choice == '9':
            print("Thank you for using Global Dorm!")
            break
        else:
            print("Invalid choice. Please try again.")

if __name__ == "__main__":
    main()