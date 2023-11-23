import MapboxCoreNavigation
import MapboxNavigation
import MapboxDirections
import MapboxMaps
import MapboxCommon
import MapboxCoreMaps

extension UIView {
    var parentViewController: UIViewController? {
        var parentResponder: UIResponder? = self
        while parentResponder != nil {
            parentResponder = parentResponder!.next
            if let viewController = parentResponder as? UIViewController {
                return viewController
            }
        }
        return nil
    }
}

@objc(MapboxNavigationView)
class MapboxNavigationView: UIView, NavigationViewControllerDelegate {
    weak var navViewController: NavigationViewController?
    let bottomBanner = {
        return CustomBottomBarViewController()
    }();
    var embedded: Bool
    var embedding: Bool
    
    var origin: NSArray = [] {
        didSet { setNeedsLayout() }
    }
    
    var destination: NSArray = [] {
        didSet { setNeedsLayout() }
    }
    
    var edges: NSDictionary?;
    var bottomConstraint: NSLayoutConstraint!
    
    @objc var shouldSimulateRoute: Bool = false
    @objc var showsEndOfRouteFeedback: Bool = false
    @objc var hideStatusView: Bool = false
    @objc var mute: Bool = false
    
    @objc var onLocationChange: RCTDirectEventBlock?
    @objc var onRouteProgressChange: RCTDirectEventBlock?
    @objc var onError: RCTDirectEventBlock?
    @objc var onCancelNavigation: RCTDirectEventBlock?
    @objc var onArrive: RCTDirectEventBlock?
    
    override init(frame: CGRect) {
        self.embedded = false
        self.embedding = false
        super.init(frame: frame)
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        
        if (navViewController == nil && !embedding && !embedded) {
            embed()
        } else {
            navViewController?.view.frame = bounds
        }
    }
    
    override func removeFromSuperview() {
        super.removeFromSuperview()
        // cleanup and teardown any existing resources
        self.navViewController?.removeFromParent()
    }
    
    private func embed() {
        guard origin.count == 2 && destination.count == 2 else { return }
        
        embedding = true
        
        let originWaypoint = Waypoint(coordinate: CLLocationCoordinate2D(latitude: origin[1] as! CLLocationDegrees, longitude: origin[0] as! CLLocationDegrees))
        let destinationWaypoint = Waypoint(coordinate: CLLocationCoordinate2D(latitude: destination[1] as! CLLocationDegrees, longitude: destination[0] as! CLLocationDegrees))
        
        let options = NavigationRouteOptions(waypoints: [originWaypoint, destinationWaypoint], profileIdentifier: .automobileAvoidingTraffic)
        
        Directions.shared.calculate(options) { [weak self] (_, result) in
            guard let strongSelf = self, let parentVC = strongSelf.parentViewController else {
                return
            }
            
            switch result {
            case .failure(let error):
                strongSelf.onError!(["message": error.localizedDescription])
            case .success(let response):
                guard let strongSelf = self else {
                    return
                }
                
                
                let indexedRouteResponse = IndexedRouteResponse(routeResponse: response, routeIndex: 0)
                
                let navigationService = MapboxNavigationService(indexedRouteResponse: indexedRouteResponse,
                                                                customRoutingProvider: NavigationSettings.shared.directions,
                                                                credentials: NavigationSettings.shared.directions.credentials,
                                                                simulating: strongSelf.shouldSimulateRoute ? .always : .onPoorGPS)
                
                
                
                let topBanner = CustomTopBarViewController()
                let bottomEdge = strongSelf.edges?.value(forKey: "bottom")
                
                let navigationOptions = NavigationOptions(styles: [CustomNightStyle()], navigationService: navigationService, topBanner: topBanner, bottomBanner: strongSelf.bottomBanner)
                let vc = NavigationViewController(for: indexedRouteResponse, navigationOptions: navigationOptions)
                
                vc.floatingButtons?[0].translatesAutoresizingMaskIntoConstraints = false;
                vc.floatingButtons?[0].topAnchor.constraint(equalTo: vc.view.topAnchor, constant: 150).isActive = true;
                vc.floatingButtons?[0].rightAnchor.constraint(equalTo: vc.view.rightAnchor, constant: -16).isActive = true;
                
                strongSelf.bottomBanner.navigationViewController = vc
                strongSelf.bottomBanner.view.heightAnchor.constraint(equalToConstant: 80).isActive = true
                strongSelf.bottomConstraint = strongSelf.bottomBanner.view.bottomAnchor.constraint(equalTo: vc.view.bottomAnchor, constant: -(bottomEdge as! CGFloat))
                
                strongSelf.bottomConstraint.isActive = true;
                
                topBanner.view.topAnchor.constraint(equalTo: vc.view.topAnchor).isActive = true
                
                vc.showsEndOfRouteFeedback = strongSelf.showsEndOfRouteFeedback
                StatusView.appearance().isHidden = strongSelf.hideStatusView
                
                NavigationSettings.shared.voiceMuted = strongSelf.mute;
                
                vc.delegate = strongSelf
                
                parentVC.addChild(vc)
                strongSelf.addSubview(vc.view)
                vc.view.frame = strongSelf.bounds
                vc.didMove(toParent: parentVC)
                strongSelf.navViewController = vc
            }
            
            strongSelf.embedding = false
            strongSelf.embedded = true
        }
    }
    
    func navigationViewController(_ navigationViewController: NavigationViewController, didUpdate progress: RouteProgress, with location: CLLocation, rawLocation: CLLocation) {
        onLocationChange?(["longitude": location.coordinate.longitude, "latitude": location.coordinate.latitude])
        onRouteProgressChange?(["distanceTraveled": progress.distanceTraveled,
                                "durationRemaining": progress.durationRemaining,
                                "fractionTraveled": progress.fractionTraveled,
                                "distanceRemaining": progress.distanceRemaining])
        
    }
    
    func navigationViewControllerDidDismiss(_ navigationViewController: NavigationViewController, byCanceling canceled: Bool) {
        if (!canceled) {
            return;
        }
        onCancelNavigation?(["message": ""]);
    }
    
    func navigationViewController(_ navigationViewController: NavigationViewController, didArriveAt waypoint: Waypoint) -> Bool {
        onArrive?(["message": ""]);
        return true;
    }
    
    // MARK: - React Native properties
    @objc func setReactEdge(_ value: NSDictionary?) {
        if(value != nil) {
            self.edges = value
            if(navViewController != nil && (value?.value(forKey: "bottom")) != nil) {
                self.bottomConstraint.constant = -(value?["bottom"] as! CGFloat);
            }
        }
    }
    
    @objc func setReactDestination(_ value: NSArray?) {
        if(value != nil) {
            self.destination = value ?? self.destination
            if(navViewController != nil) {
                let originWaypoint = Waypoint(coordinate: CLLocationCoordinate2D(latitude: origin[1] as! CLLocationDegrees, longitude: origin[0] as! CLLocationDegrees))
                let destinationWaypoint = Waypoint(coordinate: CLLocationCoordinate2D(latitude: destination[1] as! CLLocationDegrees, longitude: destination[0] as! CLLocationDegrees))
                
                let options = NavigationRouteOptions(waypoints: [originWaypoint, destinationWaypoint], profileIdentifier: .automobileAvoidingTraffic)
                
                let edges = UIEdgeInsets(top: 10, left: 10, bottom: 400, right: 20)
                
                let cameraOption = CameraOptions(center: nil, padding: edges);
                
                Directions.shared.calculate(options) { [weak self] (session, result) in
                    switch result {
                    case .failure(let error):
                        print(error.localizedDescription)
                    case .success(let response):
                        guard let strongSelf = self else {
                            return
                        }
                        
                        let indexedRouteResponse = IndexedRouteResponse(routeResponse: response, routeIndex: 0)
                        strongSelf.navViewController?.navigationService.router.updateRoute(with: indexedRouteResponse, routeOptions: options, completion: nil)
                        
                        strongSelf.navViewController?.navigationMapView?.mapView.mapboxMap.setCamera(to: cameraOption)
                    }
                }
            }
        }
    }
    
    @objc func setReactOrigin (_ value: NSArray?) {
        if(value != nil) {
            self.origin = value ?? self.origin
        }
    }
    
}


class CustomNightStyle: NightStyle {
    
    required init() {
        super.init()
    }
    
    override func apply() {
        super.apply()
        
        let traitCollection = UIScreen.main.traitCollection
        FloatingButton.appearance(for: traitCollection).backgroundColor = UIColor(hexString: "#010626")
    }
}




