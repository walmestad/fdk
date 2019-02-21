import React from 'react';
import PropTypes from 'prop-types';
import { Link } from 'react-router-dom';
import cx from 'classnames';
import _ from 'lodash';

import localization from '../../../../lib/localization';
import './search-hit-item.scss';
import { getTranslateText } from '../../../../lib/translateText';
import { SearchHitHeader } from '../../../../components/search-hit-header/search-hit-header.component';

const renderHeaderLink = (item, publisher, publishers, referenceData) => {
  if (!item) {
    return null;
  }
  const { title } = item;
  const link = `/apis/${encodeURIComponent(item.id)}`;
  return (
    <header>
      <SearchHitHeader
        tag="h2"
        title={getTranslateText(title)}
        titleLink={link}
        publisherLabel={localization.api.provider}
        publisher={publisher}
        publisherItems={publishers}
        nationalComponent={item.nationalComponent}
        statusCode={item.statusCode}
        referenceData={referenceData}
      />
    </header>
  );
};

const renderDescription = description => {
  if (!description) {
    return null;
  }
  const descriptionText = getTranslateText(description);
  const breakPattern = /(\n|<br|<\/p|<\/h)/;
  const match = breakPattern.exec(descriptionText);

  let firstLine = match
    ? descriptionText.substr(0, match.index)
    : descriptionText;

  if (firstLine.length > 250) {
    firstLine = firstLine.substr(0, 220);
    firstLine = firstLine.substr(0, firstLine.lastIndexOf(' '));
  }

  if (firstLine.length < descriptionText.length) {
    firstLine += ' ...';
  }

  return (
    <div className="fdk-text-size-medium">
      <div className="uu-invisible" aria-hidden="false">
        Beskrivelse av api
      </div>
      {firstLine}
    </div>
  );
};

const renderExpiredVersion = expired => {
  if (!expired) {
    return null;
  }
  return (
    <div className="search-hit__version mb-4 p-4">
      <span>Denne versjonen av API-et er utgått og vil fases ut i 2019. </span>
      <Link to="/TODO">Versjon 2 er dokumentert her.</Link>
    </div>
  );
};

const renderAccessRights = accessRights => {
  if (!(accessRights && accessRights[0])) {
    return null;
  }

  const { code } = accessRights[0] || {
    code: null
  };
  let accessRightsLabel;

  switch (code) {
    case 'NON_PUBLIC':
      accessRightsLabel = localization.api.accessRight.nonPublic;
      break;
    case 'RESTRICTED':
      accessRightsLabel = localization.api.accessRight.restricted;
      break;
    case 'PUBLIC':
      accessRightsLabel = localization.api.accessRight.public;
      break;
    default:
      accessRightsLabel = localization.api.accessRight.unknown;
  }

  return (
    <div className="mb-2">
      <strong>{accessRightsLabel}</strong>
    </div>
  );
};

const renderFormat = formats => {
  if (!formats || formats.length === 0) {
    return null;
  }

  const formatItems = formats =>
    formats.map((item, index) => (
      <span key={index}>
        {index > 0 ? ', ' : ''}
        {item}
      </span>
    ));

  return (
    <div className="mb-2">
      <strong>{localization.format}:&nbsp;</strong>
      {formatItems(formats)}
    </div>
  );
};

export const SearchHitItem = ({
  item,
  fadeInCounter,
  publishers,
  referenceData
}) => {
  const searchHitClass = cx('search-hit', {
    'fade-in-200': fadeInCounter === 0,
    'fade-in-300': fadeInCounter === 1,
    'fade-in-400': fadeInCounter === 2
  });

  const { isFree, isOpenAccess, isOpenLicense } = item;

  return (
    <article className={searchHitClass}>
      <span className="uu-invisible" aria-hidden="false">
        Søketreff.
      </span>

      {renderHeaderLink(
        item,
        _.get(item, 'publisher'),
        publishers,
        referenceData
      )}

      {renderExpiredVersion(_.get(item, 'expired'))}

      {renderDescription(_.get(item, 'description'))}

      {renderAccessRights(_.get(item, 'accessRights'))}

      {renderFormat(_.get(item, 'formats'))}

      {isFree === true && (
        <span className="access-icon fdk-color-green-1">
          <span className="icon2-icon-api-cost-none">
            <span className="path1" />
            <span className="path2" />
            <span className="path3" />
            <span className="path4" />
            <span className="path5" />
          </span>{' '}
          Gratis
        </span>
      )}

      {isFree === false && (
        <span className="access-icon fdk-color-red-1">
          <span className="icon2-icon-api-cost">
            <span className="path1" />
            <span className="path2" />
          </span>{' '}
          Ikke gratis å bruke
        </span>
      )}
      {isOpenAccess === true && (
        <span className="access-icon fdk-color-green-1">
          <span className="icon2-icon-api-access-all">
            <span className="path1" />
            <span className="path2" />
            <span className="path3" />
            <span className="path4" />
          </span>Åpent for alle
        </span>
      )}
      {isOpenAccess === false && (
        <span className="access-icon fdk-color-red-1">
          <span className="icon2-icon-api-access-not-limited">
            <span className="path1" />
            <span className="path2" />
          </span>Ikke åpen tilgang
        </span>
      )}
      {isOpenLicense === true && (
        <span className="access-icon fdk-color-green-1">
          <span className="icon2-icon-api-license-open">
            <span className="path1" />
            <span className="path2" />
            <span className="path3" />
            <span className="path4" />
            <span className="path5" />
            <span className="path6" />
          </span>{' '}
          Åpen lisens{' '}
        </span>
      )}
      {isOpenLicense === false && (
        <span className="access-icon fdk-color-red-1">
          <span className="icon2-icon-api-license">
            <span className="path1" />
            <span className="path2" />
            <span className="path3" />
            <span className="path4" />
            <span className="path5" />
            <span className="path6" />
          </span>{' '}
          Ikke åpen lisens{' '}
        </span>
      )}
    </article>
  );
};

SearchHitItem.defaultProps = {
  referenceData: null,
  item: {},
  publishers: null
};

SearchHitItem.propTypes = {
  item: PropTypes.shape({}),
  publishers: PropTypes.object,
  referenceData: PropTypes.object
};
