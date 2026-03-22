import React from 'react';
import './Logo.css';

// Import your image file here (make sure the extension matches what you saved)
import myLogo from '../../assets/logo-transparent.png';

export default function Logo({ animate = false, className = '', size = 24 }) {
    return (
        <img 
            src={myLogo}
            alt="C.A.F.E. Logo"
            className={`cafe-logo ${animate ? 'logo-animate' : ''} ${className}`}
            style={{ 
                width: size, 
                height: size, 
                objectFit: 'contain'
            }}
        />
    );
}
