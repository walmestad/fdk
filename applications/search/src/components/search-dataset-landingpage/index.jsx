import React from 'react';
import PropTypes from 'prop-types';

import localization from '../../components/localization';

const DatasetLandingPage = (props) => (
  <div className="dataset-landingpage">
    <div className="row fdk-row">
      {props.landingPage && props.landingPage[0] &&
      <div className="col-md-12 fdk-padding-no">
        <div className="fdk-container-detail">
          <div className="fdk-detail-icon fdk-padding-no">
            <i className="fa fa-info fdk-detail-icon-oneline" />
          </div>
          <div className="fdk-detail-text">
            <p className="fdk-ingress fdk-margin-bottom-no">
              <a
                className="dataset-landingpage-uri"
                title={localization.dataset.contactPoint.background}
                href={props.landingPage}
                target="_blank"
                rel="noopener noreferrer"
              >
                {localization.dataset.contactPoint.background}
                <i className="fa fa-external-link fdk-fa-right" />
              </a>
            </p>
          </div>
        </div>
      </div>
      }

    </div>
  </div>
)

DatasetLandingPage.defaultProps = {
  landingPage: null
};

DatasetLandingPage.propTypes = {
  landingPage: PropTypes.string
};

export default DatasetLandingPage;