import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

admin.initializeApp();

export const sendNotification = functions.database.ref('/histories/{historyId}')
  .onCreate(async (snapshot, context) => {
    const history: { [key: string]: any } = snapshot.val();
    const historyData: { [key: string]: string } = Object.keys(history).reduce(
      (acc, k) => ({ ...acc, [k]: history[k].toString() }),
      {}
    );
    console.log({ history, historyData });

    // Get the list of device notification tokens.
    const deviceTokens = await admin.database().ref(`/tokens`).once('value');
    // Listing all tokens as an array.
    const tokens: string[] = Object.keys(deviceTokens.val());
    console.log({ tokens })

    // Notification details.
    const payload: admin.messaging.MessagingPayload = {
      notification: {
        title: 'Fire...fire...fire...',
        body: `${new Date(history['time'] as number).toISOString()}-${history['verified']}`
      },
      data: <admin.messaging.DataMessagePayload>{
        ...historyData,
        id: context.params['historyId']
      },
    };

    // Send notifications to all tokens.
    const response = await admin.messaging().sendToDevice(tokens, payload);
    console.log({ response });

    // For each message check if there was an error.
    const tokensToRemove: Promise<void>[] = [];
    response.results.forEach((result, index) => {
      const error = result.error;
      if (error) {
        console.error('Failure sending notification to', tokens[index], error);
        // Cleanup the tokens who are not registered anymore.
        if (error.code === 'messaging/invalid-registration-token' ||
          error.code === 'messaging/registration-token-not-registered') {
          tokensToRemove.push(deviceTokens.ref.child(tokens[index]).remove());
        }
      }
    });
    return Promise.all(tokensToRemove);
  });