import Foundation
import UIKit
import MapboxCoreNavigation
import MapboxNavigation
import MapboxDirections

class CustomBottomBarViewController: ContainerViewController, CustomBottomBannerViewDelegate {
    
    weak var navigationViewController: NavigationViewController?
    
    // Or you can implement your own UI elements
    lazy var bannerView: CustomBottomBannerView = {
        let banner = CustomBottomBannerView()
        banner.translatesAutoresizingMaskIntoConstraints = false
        banner.delegate = self
        banner.backgroundColor = UIColor(hexString: "#010626")
        banner.layer.cornerRadius = 12
        return banner
    }()
    
    override func loadView() {
        super.loadView()
        
        view.addSubview(bannerView)
        
        let safeArea = view.layoutMarginsGuide
        NSLayoutConstraint.activate([
            bannerView.leadingAnchor.constraint(equalTo: safeArea.leadingAnchor),
            bannerView.trailingAnchor.constraint(equalTo: safeArea.trailingAnchor),
            bannerView.heightAnchor.constraint(equalTo: view.heightAnchor),
            bannerView.bottomAnchor.constraint(equalTo: safeArea.bottomAnchor)
        ])
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        setupConstraints()
    }
    
    private func setupConstraints() {
        if let superview = view.superview?.superview {
            view.bottomAnchor.constraint(equalTo: superview.safeAreaLayoutGuide.bottomAnchor).isActive = true
        }
    }
    
    // MARK: - NavigationServiceDelegate implementation
    
    func navigationService(_ service: NavigationService, didUpdate progress: RouteProgress, with location: CLLocation, rawLocation: CLLocation) {
        let km = progress.distanceRemaining / 1000
        let dr = Int(round(progress.durationRemaining / 60));
        bannerView.eta = "\(dr) min"
        bannerView.rKm = String(format: "%.2f", km) + " km"
    }
    
    // MARK: - CustomBottomBannerViewDelegate implementation
    
    func customBottomBannerDidCancel(_ banner: CustomBottomBannerView) {
        navigationViewController?.dismiss(animated: true,
                                          completion: nil)
    }
}
