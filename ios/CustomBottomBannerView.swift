import UIKit
import MapboxNavigation

protocol CustomBottomBannerViewDelegate: AnyObject {
    func customBottomBannerDidCancel(_ banner: CustomBottomBannerView)
}

class CustomBottomBannerView: UIView {
    let etaLabel: UILabel = {
        let label = UILabel();
        label.font = UIFont.systemFont(ofSize: 18, weight: .medium);
        label.textColor = UIColor.white
        label.textAlignment = .center
        label.text = "..."
        return label;
    }()
    
    let rKmLabel: UILabel = {
        let label = UILabel();
        label.font = UIFont.systemFont(ofSize: 18, weight: .medium);
        label.textColor = UIColor.white
        label.textAlignment = .center
        label.text = "..."
        return label;
    }()
    
    var eta: String? {
        get {
            return etaLabel.text
        }
        set {
            etaLabel.text = newValue
        }
    }
    
    var rKm: String? {
        get {
            return rKmLabel.text
        }
        set {
            rKmLabel.text = newValue
        }
    }
    
    weak var delegate: CustomBottomBannerViewDelegate?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        onViewLoad()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        onViewLoad()
    }
    
    
    func onViewLoad() -> Void {
        addSubview(etaLabel);
        addSubview(rKmLabel);
        etaLabel.translatesAutoresizingMaskIntoConstraints = false
        etaLabel.centerYAnchor.constraint(equalTo: self.centerYAnchor).isActive = true
        etaLabel.leftAnchor.constraint(equalTo: self.leftAnchor, constant: 100).isActive = true
        
        rKmLabel.translatesAutoresizingMaskIntoConstraints = false
        rKmLabel.rightAnchor.constraint(equalTo: self.rightAnchor, constant: -100).isActive = true
        rKmLabel.centerYAnchor.constraint(equalTo: self.centerYAnchor).isActive = true
        
        backgroundColor = UIColor.blue
    }
}
